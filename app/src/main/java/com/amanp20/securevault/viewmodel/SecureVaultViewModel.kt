package com.amanp20.securevault.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amanp20.securevault.data.model.DatabaseStatus
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.data.repository.ImportResult
import com.amanp20.securevault.data.repository.SecureVaultRepository
import kotlinx.coroutines.launch
import java.io.File

class SecureVaultViewModel(
    private val repository: SecureVaultRepository
) : ViewModel() {

    val databaseStatus: LiveData<DatabaseStatus>
        get() = repository.databaseStatus

    val documents: LiveData<List<SecureDocument>>
        get() = repository.documents

    val documentCount: LiveData<Int>
        get() = repository.documentCount

    val totalStorageUsed: LiveData<Long>
        get() = repository.totalStorageUsed

    fun importFile(
        uri: Uri,
        onResult: (ImportResult) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = repository.importFile(uri)
                onResult(result)
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun openDocument(
        document: SecureDocument,
        onResult: (File) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val file = repository.openDocument(document)
                onResult(file)
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun deleteDocument(
        document: SecureDocument,
        onResult: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteDocument(document)
                onResult()
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun renameDocument(
        document: SecureDocument,
        newName: String,
        onResult: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.renameDocument(
                    document,
                    newName
                )
                onResult()
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun verifyPin(
        pin: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val valid = repository.verifyPin(pin)
                onResult(valid)
            } catch (_: Throwable) {
                onResult(false)
            }
        }
    }

    fun saveNewPin(
        pin: String,
        pinLength: Int,
        onResult: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.saveNewPin(
                    pin,
                    pinLength
                )
                onResult()
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun changePin(
        currentPin: String,
        newPin: String,
        newPinLength: Int,
        onResult: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.changePin(
                    currentPin,
                    newPin,
                    newPinLength
                )
                onResult()
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun hasStoredPin(): Boolean {
        return repository.hasStoredPin()
    }

    fun getStoredPinLength(): Int {
        return repository.getStoredPinLength()
    }

    fun isBiometricEnabled(): Boolean {
        return repository.isBiometricEnabled()
    }

    fun setBiometricEnabled(
        enabled: Boolean
    ) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
        }
    }
}

class SecureVaultViewModelFactory(
    private val repository: SecureVaultRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                SecureVaultViewModel::class.java
            )
        ) {
            return SecureVaultViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}