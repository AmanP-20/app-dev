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
import com.amanp20.securevault.utils.SecurityPreferencesManager
import com.amanp20.securevault.utils.addSourceLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecureVaultRepositoryImpl(context: Context) : SecureVaultRepository {
    private val applicationContext = context.applicationContext
    private val preferencesManager = SecurityPreferencesManager(applicationContext)
    private val securityManager = DocumentSecurityManager(applicationContext)
    private val databaseStatusMutable = MutableLiveData<DatabaseStatus>()
    private val documentCountMutable = MutableLiveData(0)
    private val noteCountMutable = MutableLiveData(0)

    private val database: SecureVaultDatabase? = runCatching {
        SecureVaultDatabase.getInstance(applicationContext)
    }.onSuccess { db ->
        databaseStatusMutable.value = DatabaseStatus.Ready
        documentCountMutable.addSourceLiveData(db.secureDocumentDao().observeDocumentCount())
        noteCountMutable.addSourceLiveData(db.secureNoteDao().observeNoteCount())
    }.getOrElse {
        databaseStatusMutable.value = DatabaseStatus.Error(
            applicationContext.getString(R.string.error_database_initialization_failed)
        )
        null
    }

    override val databaseStatus: LiveData<DatabaseStatus> = databaseStatusMutable
    override val documentCount: LiveData<Int> = documentCountMutable
    override val noteCount: LiveData<Int> = noteCountMutable
    override val documents: LiveData<List<SecureDocument>> =
        database?.secureDocumentDao()?.observeAll() ?: MutableLiveData(emptyList())

    override fun hasStoredPin() = preferencesManager.hasStoredPin()
    override fun getStoredPinLength() = preferencesManager.getStoredPinLength()
    override fun isBiometricEnabled() = preferencesManager.isBiometricEnabled()

    override suspend fun saveNewPin(pin: String, pinLength: Int) = withContext(Dispatchers.IO) {
        preferencesManager.savePin(pin, pinLength)
    }

    override suspend fun verifyPin(pin: String) = withContext(Dispatchers.IO) {
        preferencesManager.verifyPin(pin)
    }

    override suspend fun changePin(currentPin: String, newPin: String, newPinLength: Int) =
        withContext(Dispatchers.IO) {
            check(preferencesManager.verifyPin(currentPin)) {
                applicationContext.getString(R.string.error_wrong_pin)
            }
            preferencesManager.savePin(newPin, newPinLength)
        }

    override suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setBiometricEnabled(enabled)
    }

    override suspend fun addDocument(document: SecureDocument): AddDocumentResult =
        withContext(Dispatchers.IO) {
            val dao = requireDao()
            if (dao.existsByUri(document.uri)) AddDocumentResult.Duplicate
            else {
                dao.insert(document)
                AddDocumentResult.Added
            }
        }

    override suspend fun deleteDocument(document: SecureDocument) = withContext(Dispatchers.IO) {
        securityManager.removeProtection(document.encryptedFilePath)
        requireDao().deleteById(document.id)
    }

    override suspend fun lockDocument(document: SecureDocument, lockType: String, password: String?): SecureDocument =
        withContext(Dispatchers.IO) {
            check(!document.isLocked) { "Document is already locked" }
            val encrypted = securityManager.encryptDocument(document.id, Uri.parse(document.uri))
            val salt = password?.let { ByteArray(16).also(SecureRandom()::nextBytes) }
            val hash = if (password != null && salt != null) hashPassword(password, salt) else null
            val locked = document.copy(
                isLocked = true,
                encryptedFilePath = encrypted.path,
                encryptionIv = encrypted.iv,
                lockedAt = System.currentTimeMillis(),
                lockType = lockType,
                passwordSalt = salt?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                passwordHash = hash,
                failedAttempts = 0,
                blockedUntil = null
            )
            try {
                requireDao().update(locked)
                locked
            } catch (error: Throwable) {
                securityManager.removeProtection(encrypted.path)
                throw error
            }
        }

    override suspend fun unlockDocument(document: SecureDocument, password: String?): Uri = withContext(Dispatchers.IO) {
        check(document.isLocked && document.encryptedFilePath != null && document.encryptionIv != null) {
            "Document is not protected"
        }
        check(document.blockedUntil == null || document.blockedUntil <= System.currentTimeMillis()) {
            "Document authentication temporarily blocked"
        }
        if (document.passwordHash != null && password != null) {
            val salt = Base64.decode(requireNotNull(document.passwordSalt), Base64.NO_WRAP)
            if (hashPassword(password, salt) != document.passwordHash) {
                val attempts = document.failedAttempts + 1
                requireDao().update(
                    document.copy(
                        failedAttempts = attempts,
                        blockedUntil = if (attempts >= 5) System.currentTimeMillis() + 30_000 else null
                    )
                )
                error("Incorrect document password")
            }
            requireDao().update(document.copy(failedAttempts = 0, blockedUntil = null))
        } else if (document.passwordHash != null) {
            error("Document password required")
        }
        securityManager.decryptDocument(
            document.id,
            document.encryptedFilePath!!,
            document.encryptionIv!!
        )
    }

    override suspend fun removeDocumentProtection(document: SecureDocument): SecureDocument =
        withContext(Dispatchers.IO) {
            securityManager.removeProtection(document.encryptedFilePath)
            val unprotected = document.copy(
                isLocked = false,
                encryptedFilePath = null,
                encryptionIv = null,
                lockedAt = null,
                lockType = "NONE",
                passwordSalt = null,
                passwordHash = null,
                failedAttempts = 0,
                blockedUntil = null
            )
            requireDao().update(unprotected)
            unprotected
        }

    private fun requireDao() = requireNotNull(database) {
        applicationContext.getString(R.string.error_database_initialization_failed)
    }.secureDocumentDao()

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return Base64.encodeToString(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
            Base64.NO_WRAP
        )
    }
}
