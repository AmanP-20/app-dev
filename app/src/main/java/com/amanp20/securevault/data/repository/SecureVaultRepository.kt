package com.amanp20.securevault.data.repository

import androidx.lifecycle.LiveData
import android.net.Uri
import com.amanp20.securevault.data.model.DatabaseStatus
import com.amanp20.securevault.data.model.SecureDocument

interface SecureVaultRepository {

    val databaseStatus: LiveData<DatabaseStatus>
    val documentCount: LiveData<Int>
    val noteCount: LiveData<Int>

    fun hasStoredPin(): Boolean
    fun getStoredPinLength(): Int
    fun isBiometricEnabled(): Boolean

    suspend fun saveNewPin(pin: String, pinLength: Int)
    suspend fun verifyPin(pin: package com.amanp20.securevault.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.amanp20.securevault.data.model.DatabaseStatus
import com.amanp20.securevault.data.model.SecureDocument
import java.io.File

interface SecureVaultRepository {

    val databaseStatus: LiveData<DatabaseStatus>

    val documentCount: LiveData<Int>

    val totalStorageUsed: LiveData<Long>

    val documents: LiveData<List<SecureDocument>>

    fun hasStoredPin(): Boolean

    fun getStoredPinLength(): Int

    fun isBiometricEnabled(): Boolean

    suspend fun saveNewPin(
        pin: String,
        pinLength: Int
    )

    suspend fun verifyPin(
        pin: String
    ): Boolean

    suspend fun changePin(
        currentPin: String,
        newPin: String,
        newPinLength: Int
    )

    suspend fun setBiometricEnabled(
        enabled: Boolean
    )

    suspend fun importFile(
        uri: Uri
    ): ImportResult

    suspend fun openDocument(
        document: SecureDocument
    ): File

    suspend fun deleteDocument(
        document: SecureDocument
    )

    suspend fun renameDocument(
        document: SecureDocument,
        newName: String
    )
}

sealed interface ImportResult {

    data object Added : ImportResult

    data object Duplicate : ImportResult
}String): Boolean
    suspend fun changePin(currentPin: String, newPin: String, newPinLength: Int)
    suspend fun setBiometricEnabled(enabled: Boolean)

    val documents: LiveData<List<SecureDocument>>
    suspend fun addDocument(document: SecureDocument): AddDocumentResult
    suspend fun deleteDocument(document: SecureDocument)
    suspend fun lockDocument(document: SecureDocument, lockType: String, password: String?): SecureDocument
    suspend fun unlockDocument(document: SecureDocument, password: String?): Uri
    suspend fun removeDocumentProtection(document: SecureDocument): SecureDocument
}

sealed interface AddDocumentResult {
    data object Added : AddDocumentResult
    data object Duplicate : AddDocumentResult
}
