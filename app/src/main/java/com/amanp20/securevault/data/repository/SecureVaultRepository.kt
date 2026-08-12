package com.amanp20.securevault.data.repository

import androidx.lifecycle.LiveData
import com.amanp20.securevault.data.model.DatabaseStatus

interface SecureVaultRepository {

    val databaseStatus: LiveData<DatabaseStatus>
    val documentCount: LiveData<Int>
    val noteCount: LiveData<Int>

    fun hasStoredPin(): Boolean
    fun getStoredPinLength(): Int
    fun isBiometricEnabled(): Boolean

    suspend fun saveNewPin(pin: String, pinLength: Int)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun changePin(currentPin: String, newPin: String, newPinLength: Int)
    suspend fun setBiometricEnabled(enabled: Boolean)
}
