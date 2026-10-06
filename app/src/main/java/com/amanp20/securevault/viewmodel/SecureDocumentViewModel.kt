package com.amanp20.securevault.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.data.repository.ImportResult
import com.amanp20.securevault.data.repository.SecureVaultRepository
import kotlinx.coroutines.launch
import java.io.File

class SecureDocumentViewModel(
    private val repository: SecureVaultRepository
) : ViewModel() {

    val documents:
        LiveData<List<SecureDocument>>
        get() = repository.documents

    val documentCount:
        LiveData<Int>
        get() = repository.documentCount

    val totalStorageUsed:
        LiveData<Long>
        get() = repository.totalStorageUsed

    fun importFile(
        uri: Uri,
        onResult: (ImportResult) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {

            try {

                val result =
                    repository.importFile(uri)

                onResult(result)

            } catch (error: Throwable) {

                onError(error)
            }
        }
    }

    fun getDocumentById(
        id: Long,
        onResult: (SecureDocument?) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {

            try {

                val document =
                    repository.getDocumentById(id)

                onResult(document)

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

                val file =
                    repository.openDocument(
                        document
                    )

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

                repository.deleteDocument(
                    document
                )

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
}

class SecureDocumentViewModelFactory(
    private val repository: SecureVaultRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                SecureDocumentViewModel::class.java
            )
        ) {

            return SecureDocumentViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}