package com.amanp20.securevault.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {

    private const val ALGORITHM =
        "PBKDF2WithHmacSHA256"

    private const val ITERATION_COUNT =
        120_000

    private const val KEY_LENGTH =
        256

    private const val SALT_LENGTH =
        16

    fun generateSalt(): String {

        val salt =
            ByteArray(SALT_LENGTH)

        SecureRandom().nextBytes(
            salt
        )

        return Base64.encodeToString(
            salt,
            Base64.NO_WRAP
        )
    }

    fun hashPin(
        pin: String,
        saltBase64: String
    ): String {

        require(pin.isNotEmpty()) {
            "PIN cannot be empty"
        }

        val salt =
            Base64.decode(
                saltBase64,
                Base64.NO_WRAP
            )

        require(
            salt.size == SALT_LENGTH
        ) {
            "Invalid PIN salt"
        }

        val spec =
            PBEKeySpec(
                pin.toCharArray(),
                salt,
                ITERATION_COUNT,
                KEY_LENGTH
            )

        return try {

            val factory =
                SecretKeyFactory.getInstance(
                    ALGORITHM
                )

            Base64.encodeToString(
                factory
                    .generateSecret(spec)
                    .encoded,
                Base64.NO_WRAP
            )

        } finally {

            spec.clearPassword()
        }
    }

    fun verifyPin(
        pin: String,
        saltBase64: String,
        expectedHash: String
    ): Boolean {

        if (
            pin.isEmpty() ||
            saltBase64.isEmpty() ||
            expectedHash.isEmpty()
        ) {
            return false
        }

        return try {

            val actualHash =
                Base64.decode(
                    hashPin(
                        pin,
                        saltBase64
                    ),
                    Base64.NO_WRAP
                )

            val storedHash =
                Base64.decode(
                    expectedHash,
                    Base64.NO_WRAP
                )

            MessageDigest.isEqual(
                actualHash,
                storedHash
            )

        } catch (_: IllegalArgumentException) {

            false
        }
    }
}