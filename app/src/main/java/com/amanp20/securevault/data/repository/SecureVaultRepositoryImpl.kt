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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureVaultRepositoryImpl(
    context: Context
) : SecureVaultRepository {

    private val appContext =
        context.applicationContext

    private val preferences =
        SecurityPreferencesManager(
            appContext
        )

    private val securityManager =
        DocumentSecurityManager(
            appContext
        )

    private val databaseStatusMutable =
        MutableLiveData<DatabaseStatus>()

    private val database =
        runCatching {

            SecureVaultDatabase.getInstance(
                appContext
            )

        }.onSuccess {

            databaseStatusMutable.postValue(
                DatabaseStatus.Ready
            )

        }.onFailure {

            databaseStatusMutable.postValue(
                DatabaseStatus.Error(
                    appContext.getString(
                        R.string.error_database_initialization_failed
                    )
                )
            )
        }.getOrNull()

    private val dao
        get() = database?.secureDocumentDao()

    override val databaseStatus:
        LiveData<DatabaseStatus> =
        databaseStatusMutable

    override val documents:
        LiveData<List<SecureDocument>> =
        dao?.observeAll()
            ?: MutableLiveData(emptyList())

    override val documentCount:
        LiveData<Int> =
        dao?.observeDocumentCount()
            ?: MutableLiveData(0)

    override val totalStorageUsed:
        LiveData<Long> =
        dao?.observeTotalSize()
            ?: MutableLiveData(0L)

    override fun hasStoredPin(): Boolean =
        preferences.hasStoredPin()

    override fun getStoredPinLength(): Int =
        preferences.getStoredPinLength()

    override fun isBiometricEnabled(): Boolean =
        preferences.isBiometricEnabled()

    override suspend fun saveNewPin(
        pin: String,
        pinLength: Int
    ) = withContext(Dispatchers.IO) {

        preferences.savePin(
            pin,
            pinLength
        )
    }

    override suspend fun verifyPin(
        pin: String
    ): Boolean =
        withContext(Dispatchers.IO) {

            preferences.verifyPin(
                pin
            )
        }

    override suspend fun changePin(
        currentPin: String,
        newPin: String,
        newPinLength: Int
    ) = withContext(Dispatchers.IO) {

        check(
            preferences.verifyPin(
                currentPin
            )
        ) {
            appContext.getString(
                R.string.error_wrong_pin
            )
        }

        preferences.savePin(
            newPin,
            newPinLength
        )
    }

    override suspend fun setBiometricEnabled(
        enabled: Boolean
    ) = withContext(Dispatchers.IO) {

        preferences.setBiometricEnabled(
            enabled
        )
    }

    override suspend fun importFile(
        uri: Uri
    ): ImportResult =
        withContext(Dispatchers.IO) {

            val documentDao =
                requireNotNull(dao)

            val metadata =
                FileMetadataUtils.readMetadata(
                    appContext,
                    uri
                )

            if (
                documentDao.existsByName(
                    metadata.name
                )
            ) {
                return@withContext ImportResult.Duplicate
            }

            val encryptedName =
                "${System.currentTimeMillis()}_${
                    System.nanoTime()
                }.enc"

            val encrypted =
                appContext
                    .contentResolver
                    .openInputStream(uri)
                    ?.use { input ->

                        securityManager.encrypt(
                            input,
                            encryptedName
                        )

                    }
                    ?: error(
                        "Unable to open selected file"
                    )

            try {

                documentDao.insert(
                    SecureDocument(
                        originalName =
                            metadata.name,

                        encryptedFilePath =
                            encrypted.path,

                        encryptionIv =
                            encrypted.iv,

                        mimeType =
                            metadata.mimeType,

                        fileSize =
                            metadata.size,

                        category =
                            metadata.category,

                        dateAdded =
                            System.currentTimeMillis(),

                        lastModified =
                            metadata.lastModified
                    )
                )

                ImportResult.Added

            } catch (error: Throwable) {

                securityManager
                    .deleteEncryptedFile(
                        encrypted.path
                    )

                throw error
            }
        }

    override suspend fun getDocumentById(
        id: Long
    ): SecureDocument? =
        withContext(Dispatchers.IO) {

            requireNotNull(dao)
                .getById(id)
        }

    override suspend fun openDocument(
        document: SecureDocument
    ): java.io.File =
        withContext(Dispatchers.IO) {

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

        requireNotNull(dao)
            .delete(document)
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

        val documentDao =
            requireNotNull(dao)

        require(
            cleanedName == document.originalName ||
                !documentDao.existsByName(
                    cleanedName
                )
        ) {
            "A file with this name already exists"
        }

        documentDao.update(
            document.copy(
                originalName =
                    cleanedName,

                lastModified =
                    System.currentTimeMillis()
            )
        )
    }
}