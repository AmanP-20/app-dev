package com.amanp20.securevault.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityPreferencesManager(
    context: Context
) {

    private val preferences:
        SharedPreferences

    init {

        val masterKey =
            MasterKey.Builder(context)
                .setKeyScheme(
                    MasterKey.KeyScheme.AES256_GCM
                )
                .build()

        preferences =
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences
                    .PrefKeyEncryptionScheme
                    .AES256_SIV,
                EncryptedSharedPreferences
                    .PrefValueEncryptionScheme
                    .AES256_GCM
            )
    }

    fun hasStoredPin(): Boolean {

        return preferences.contains(
            KEY_PIN_HASH
        ) &&
            preferences.contains(
                KEY_PIN_SALT
            )
    }

    fun getStoredPinLength(): Int {

        return preferences.getInt(
            KEY_PIN_LENGTH,
            0
        )
    }

    fun savePin(
        pin: String,
        pinLength: Int
    ) {

        val salt =
            PinHasher.generateSalt()

        val hash =
            PinHasher.hashPin(
                pin,
                salt
            )

        preferences
            .edit()
            .putString(
                KEY_PIN_HASH,
                hash
            )
            .putString(
                KEY_PIN_SALT,
                salt
            )
            .putInt(
                KEY_PIN_LENGTH,
                pinLength
            )
            .putBoolean(
                KEY_BIOMETRIC_ENABLED,
                false
            )
            .apply()
    }

    fun verifyPin(
        pin: String
    ): Boolean {

        val salt =
            preferences.getString(
                KEY_PIN_SALT,
                null
            ) ?: return false

        val hash =
            preferences.getString(
                KEY_PIN_HASH,
                null
            ) ?: return false

        return PinHasher.verifyPin(
            pin,
            salt,
            hash
        )
    }

    fun isBiometricEnabled(): Boolean {

        return preferences.getBoolean(
            KEY_BIOMETRIC_ENABLED,
            false
        )
    }

    fun setBiometricEnabled(
        enabled: Boolean
    ) {

        if (
            enabled &&
            !hasStoredPin()
        ) {
            return
        }

        preferences
            .edit()
            .putBoolean(
                KEY_BIOMETRIC_ENABLED,
                enabled
            )
            .apply()
    }

    companion object {

        private const val FILE_NAME =
            "secure_vault_secure_prefs"

        private const val KEY_PIN_HASH =
            "pin_hash"

        private const val KEY_PIN_SALT =
            "pin_salt"

        private const val KEY_PIN_LENGTH =
            "pin_length"

        private const val KEY_BIOMETRIC_ENABLED =
            "biometric_enabled"
    }
}