package com.amanp20.securevault.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.data.repository.AddDocumentResult
import com.amanp20.securevault.data.repository.SecureVaultRepository
import com.amanp20.securevault.utils.FileMetadataUtils
import kotlinx.coroutines.launch

enum class DocumentSort { NAME_ASC, NAME_DESC, NEWEST, OLDEST, LARGEST, SMALLEST }

class SecureDocumentViewModel(private val repository: SecureVaultRepository) : ViewModel() {
    private val query = MutableLiveData("")
    private val category = MutableLiveData("All")
    private val sort = MutableLiveData(DocumentSort.NEWEST)
    private val _documents = MediatorLiveData<List<SecureDocument>>()
    private val _message = MutableLiveData<String>()

    val documents: LiveData<List<SecureDocument>> = _documents
    val message: LiveData<String> = _message

    init {
        listOf(repository.documents, query, category, sort).forEach { source ->
            _documents.addSource(source) { refresh() }
        }
    }

    fun setQuery(value: String) { query.value = value }
    fun setCategory(value: String) { category.value = value }
    fun setSort(value: DocumentSort) { sort.value = value }

    fun importUris(resolver: ContentResolver, uris: List<Uri>) {
        uris.forEach { uri ->
            viewModelScope.launch {
                try {
                    resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Some providers do not offer persistable permissions; the URI can still be used now.
                } catch (_: IllegalArgumentException) {
                    // The provider did not grant persistable access for this URI.
                }
                val document = runCatching { FileMetadataUtils.readDocument(resolver, uri) }.getOrNull()
                if (document == null) {
                    _message.value = "This file cannot be added to your vault."
                } else if (repository.addDocument(document) is AddDocumentResult.Duplicate) {
                    _message.value = "This file is already in your vault."
                }
            }
        }
    }

    fun delete(document: SecureDocument) {
        viewModelScope.launch {
            runCatching { repository.deleteDocument(document) }
                .onFailure { _message.value = "The document could not be deleted." }
        }
    }

    fun removeProtection(document: SecureDocument, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { repository.removeDocumentProtection(document) }
                .onSuccess { onSuccess?.invoke() }
                .onFailure { _message.value = "The document protection could not be removed." }
        }
    }

    fun lock(document: SecureDocument, lockType: String, password: String? = null) {
        viewModelScope.launch {
            runCatching { repository.lockDocument(document, lockType, password) }
                .onFailure { _message.value = "The document could not be protected." }
        }
    }

    fun unlock(document: SecureDocument, password: String?, onSuccess: (android.net.Uri) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.unlockDocument(document, password) }
                .onSuccess(onSuccess)
                .onFailure { _message.value = if (it.message == "Incorrect document password") it.message else "The document could not be opened." }
        }
    }

    private fun refresh() {
        val search = query.value.orEmpty().trim()
        val selectedCategory = category.value ?: "All"
        _documents.value = repository.documents.value.orEmpty()
            .filter { search.isEmpty() || it.name.contains(search, ignoreCase = true) }
            .filter { selectedCategory == "All" || it.category == selectedCategory }
            .let { items ->
                when (sort.value) {
                    DocumentSort.NAME_ASC -> items.sortedBy { it.name.lowercase() }
                    DocumentSort.NAME_DESC -> items.sortedByDescending { it.name.lowercase() }
                    DocumentSort.OLDEST -> items.sortedBy { it.dateAdded }
                    DocumentSort.LARGEST -> items.sortedByDescending { it.fileSize }
                    DocumentSort.SMALLEST -> items.sortedBy { it.fileSize }
                    else -> items.sortedByDescending { it.dateAdded }
                }
            }
    }
}
