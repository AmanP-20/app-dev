package com.amanp20.securevault.data.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amanp20.securevault.R
import com.amanp20.securevault.data.database.SecureVaultDatabase
import com.amanp20.securevault.data.model.DatabaseStatus
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.data.security.DocumentSecurityManager
import com.amanp20.securevault.utils.FileMetadataUtils
import com.amanp20.securevault.utils.SecurityPreferencesManager
import com.amanp20.securevault.utils.addSourceLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SecureVaultRepositoryImpl(
    context: Context
) : SecureVaultRepository {

    private val applicationContext =
        context.applicationContext

    private val preferencesManager =
        SecurityPreferencesManager(applicationContext)

    private val securityManager =
        DocumentSecurityManager(applicationContext)

    private val databaseStatusMutable =
        MutableLiveData<DatabaseStatus>()

    private val documentCountMutable =
        MutableLiveData(0)

    private val totalStorageMutable =
        MutableLiveData(0L)

    private val database: SecureVaultDatabase? =
        runCatching {

            SecureVaultDatabase.getInstance(
                applicationContext
            )

        }.onSuccess { db ->

            databaseStatusMutable.value =
                DatabaseStatus.Ready

            documentCountMutable.addSourceLiveData(
                db.secureDocumentDao()
                    .observeDocumentCount()
            )

            totalStorageMutable.addSourceLiveData(
                db.secureDocumentDao()
                    .observeTotalSize()
            )

        }.getOrElse {

            databaseStatusMutable.value =
                DatabaseStatus.Error(
                    applicationContext.getString(
                        R.string.error_database_initialization_failed
                    )
                )

            null
        }

    override val databaseStatus:
        LiveData<DatabaseStatus> =
        databaseStatusMutable

    override val documentCount:
        LiveData<Int> =
        documentCountMutable

    override val totalStorageUsed:
        LiveData<Long> =
        totalStorageMutable

    override val documents:
        LiveData<List<SecureDocument>> =
        database
            ?.secureDocumentDao()
            ?.observeAll()
            ?: MutableLiveData(emptyList())

    override fun hasStoredPin(): Boolean =
        preferencesManager.hasStoredPin()

    override fun getStoredPinLength(): Int =
        preferencesManager.getStoredPinLength()

    override fun isBiometricEnabled(): Boolean =
        preferencesManager.isBiometricEnabled()

    override suspend fun saveNewPin(
        pin: String,
        pinLength: Int
    ) = withContext(Dispatchers.IO) {

        preferencesManager.savePin(
            pin,
            pinLength
        )
    }

    override suspend fun verifyPin(
        pin: String
    ): Boolean = withContext(Dispatchers.IO) {

        preferencesManager.verifyPin(pin)
    }

    override suspend fun changePin(
        currentPin: String,
        newPin: String,
        newPinLength: Int
    ) = withContext(Dispatchers.IO) {

        check(
            preferencesManager.verifyPin(currentPin)
        ) {
            applicationContext.getString(
                R.string.error_wrong_pin
            )
        }

        preferencesManager.savePin(
            newPin,
            newPinLength
        )
    }

    override suspend fun setBiometricEnabled(
        enabled: Boolean
    ) = withContext(Dispatchers.IO) {

        preferencesManager.setBiometricEnabled(
            enabled
        )
    }

    override suspend fun importFile(
        uri: Uri
    ): ImportResult = withContext(Dispatchers.IO) {

        val dao = requireDao()

        val metadata =
            FileMetadataUtils.readMetadata(
                applicationContext,
                uri
            )

        val originalName =
            metadata.name

        if (dao.existsByName(originalName)) {
            return@withContext ImportResult.Duplicate
        }

        val inputStream =
            applicationContext.contentResolver
                .openInputStream(uri)
                ?: error(
                    "Unable to open selected file"
                )

        val encryptedName =
            "${System.currentTimeMillis()}_${System.nanoTime()}.enc"

        val encrypted =
            try {

                securityManager.encrypt(
                    input = inputStream,
                    fileName = encryptedName
                )

            } catch (error: Throwable) {

                inputStream.close()

                throw error
            }

        val document =
            SecureDocument(
                originalName = originalName,
                encryptedFilePath = encrypted.path,
                encryptionIv = encrypted.iv,
                mimeType = metadata.mimeType,
                fileSize = metadata.size,
                category = metadata.category,
                dateAdded = System.currentTimeMillis(),
                lastModified = metadata.lastModified
            )

        try {

            dao.insert(document)

            ImportResult.Added

        } catch (error: Throwable) {

            securityManager.deleteEncryptedFile(
                encrypted.path
            )

            throw error
        }
    }

    override suspend fun openDocument(
        document: SecureDocument
    ): File = withContext(Dispatchers.IO) {

        securityManager.decryptToCache(
            encryptedPath =
                document.encryptedFilePath,

            encodedIv =
                document.encryptionIv,

            outputName =
                document.originalName
        )
    }

    override suspend fun deleteDocument(
        document: SecureDocument
    ) = withContext(Dispatchers.IO) {

        securityManager.deleteEncryptedFile(
            document.encryptedFilePath
        )

        requireDao().delete(
            document
        )
    }

    override suspend fun renameDocument(
        document: SecureDocument,
        newName: String
    ) = withContext(Dispatchers.IO) {

        val cleanedName =
            newName.trim()

        require(
            cleanedName.isNotBlank()
        ) {
            "File name cannot be empty"
        }

        require(
            !requireDao().existsByName(
                cleanedName
            )
        ) {
            "A file with this name already exists"
        }

        requireDao().update(
            document.copy(
                originalName = cleanedName,
                lastModified =
                    System.currentTimeMillis()
            )
        )
    }

    private fun requireDao() =
        requireNotNull(database) {

            applicationContext.getString(
                R.string.error_database_initialization_failed
            )

        }.secureDocumentDao()
}