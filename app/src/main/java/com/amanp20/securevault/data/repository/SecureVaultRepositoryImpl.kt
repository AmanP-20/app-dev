package com.amanp20.securevault.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amanp20.securevault.R
import com.amanp20.securevault.data.database.SecureVaultDatabase
import com.amanp20.securevault.data.model.DatabaseStatus
import com.amanp20.securevault.utils.addSourceLiveData
import com.amanp20.securevault.utils.SecurityPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureVaultRepositoryImpl(context: Context) : SecureVaultRepository {

    private val applicationContext = context.applicationContext
    private val preferencesManager = SecurityPreferencesManager(applicationContext)
    private val databaseStatusMutable = MutableLiveData<DatabaseStatus>()
    private val documentCountMutable = MutableLiveData(0)
    private val noteCountMutable = MutableLiveData(0)

    private val database: SecureVaultDatabase? = runCatching {
        SecureVaultDatabase.getInstance(applicationContext)
    }.onSuccess { secureVaultDatabase ->
        databaseStatusMutable.value = DatabaseStatus.Ready
        documentCountMutable.addSourceLiveData(secureVaultDatabase.secureDocumentDao().observeDocumentCount())
        noteCountMutable.addSourceLiveData(secureVaultDatabase.secureNoteDao().observeNoteCount())
    }.getOrElse {
        databaseStatusMutable.value = DatabaseStatus.Error(
            applicationContext.getString(R.string.error_database_initialization_failed)
        )
        null
    }

    override val databaseStatus: LiveData<DatabaseStatus> = databaseStatusMutable
    override val documentCount: LiveData<Int> = documentCountMutable
    override val noteCount: LiveData<Int> = noteCountMutable

    override fun hasStoredPin(): Boolean = preferencesManager.hasStoredPin()

    override fun getStoredPinLength(): Int = preferencesManager.getStoredPinLength()

    override fun isBiometricEnabled(): Boolean = preferencesManager.isBiometricEnabled()

    override suspend fun saveNewPin(pin: String, pinLength: Int) {
        withContext(Dispatchers.IO) {
            preferencesManager.savePin(pin, pinLength)
        }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        return withContext(Dispatchers.IO) {
            preferencesManager.verifyPin(pin)
        }
    }

    override suspend fun changePin(currentPin: String, newPin: String, newPinLength: Int) {
        withContext(Dispatchers.IO) {
            check(preferencesManager.verifyPin(currentPin)) {
                applicationContext.getString(R.string.error_wrong_pin)
            }
            preferencesManager.savePin(newPin, newPinLength)
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesManager.setBiometricEnabled(enabled)
        }
    }
}
