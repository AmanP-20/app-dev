package com.amanp20.securevault.data.repository

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
}