package com.amanp20.securevault.utils

import android.content.Context
import androidx.biometric.BiometricManager

object BiometricCapability {

    fun canAuthenticate(context: Context): Int {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
    }

    fun isAvailable(context: Context): Boolean {
        return canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
