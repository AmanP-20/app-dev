package com.amanp20.securevault.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasStoredPin(): Boolean {
        return sharedPreferences.contains(KEY_PIN_HASH) && sharedPreferences.contains(KEY_PIN_SALT)
    }

    fun getStoredPinLength(): Int {
        return sharedPreferences.getInt(KEY_PIN_LENGTH, 0)
    }

    fun savePin(pin: String, pinLength: Int) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin(pin, salt)
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putInt(KEY_PIN_LENGTH, pinLength)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedSalt = sharedPreferences.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false
        return PinHasher.verifyPin(pin, storedSalt, storedHash)
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    companion object {
        private const val FILE_NAME = "secure_vault_secure_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
