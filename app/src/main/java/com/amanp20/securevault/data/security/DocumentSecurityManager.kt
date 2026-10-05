package com.amanp20.securevault.data.security

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class DocumentSecurityManager(
    context: Context
) {

    private val appContext = context.applicationContext

    private val vaultDirectory = File(
        appContext.filesDir,
        "vault"
    ).apply {
        mkdirs()
    }

    private val previewDirectory = File(
        appContext.cacheDir,
        "vault_previews"
    ).apply {
        mkdirs()
    }

    fun encrypt(
        input: InputStream,
        fileName: String
    ): EncryptedDocument {

        val destination = File(
            vaultDirectory,
            fileName
        )

        val iv = ByteArray(GCM_IV_LENGTH)

        SecureRandom().nextBytes(iv)

        val cipher = createCipher(
            mode = Cipher.ENCRYPT_MODE,
            iv = iv
        )

        try {

            input.use { source ->

                destination.outputStream().use { output ->

                    CipherOutputStream(
                        output,
                        cipher
                    ).use { encryptedOutput ->

                        source.copyTo(encryptedOutput)
                    }
                }
            }

        } catch (error: Throwable) {

            destination.delete()

            throw error
        }

        return EncryptedDocument(
            path = destination.absolutePath,
            iv = Base64.encodeToString(
                iv,
                Base64.NO_WRAP
            )
        )
    }

    fun decryptToCache(
        encryptedPath: String,
        encodedIv: String,
        outputName: String
    ): File {

        val encryptedFile = File(encryptedPath)

        require(encryptedFile.isFile) {
            "Encrypted file does not exist"
        }

        clearExpiredPreviews()

        val outputFile = File(
            previewDirectory,
            "${System.currentTimeMillis()}_$outputName"
        )

        val iv = Base64.decode(
            encodedIv,
            Base64.NO_WRAP
        )

        val cipher = createCipher(
            mode = Cipher.DECRYPT_MODE,
            iv = iv
        )

        try {

            encryptedFile.inputStream().use { input ->

                CipherInputStream(
                    input,
                    cipher
                ).use { decryptedInput ->

                    outputFile.outputStream().use { output ->

                        decryptedInput.copyTo(output)
                    }
                }
            }

        } catch (error: Throwable) {

            outputFile.delete()

            throw error
        }

        return outputFile
    }

    fun deleteEncryptedFile(
        encryptedPath: String?
    ) {

        if (encryptedPath.isNullOrBlank()) {
            return
        }

        File(encryptedPath).delete()
    }

    fun clearPreviewCache() {

        previewDirectory
            .listFiles()
            ?.forEach { file ->
                file.delete()
            }
    }

    private fun clearExpiredPreviews() {

        val expiry =
            System.currentTimeMillis() - PREVIEW_RETENTION_MILLIS

        previewDirectory
            .listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.lastModified() < expiry
            }
            ?.forEach { file ->
                file.delete()
            }
    }

    private fun createCipher(
        mode: Int,
        iv: ByteArray
    ): Cipher {

        return Cipher
            .getInstance(TRANSFORMATION)
            .apply {

                init(
                    mode,
                    getKey(),
                    GCMParameterSpec(
                        GCM_TAG_LENGTH,
                        iv
                    )
                )
            }
    }

    private fun getKey(): SecretKey {

        val keyStore = KeyStore
            .getInstance(ANDROID_KEYSTORE)
            .apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        return KeyGenerator
            .getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            .apply {

                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(
                            KeyProperties.BLOCK_MODE_GCM
                        )
                        .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setRandomizedEncryptionRequired(false)
                        .build()
                )
            }
            .generateKey()
    }

    data class EncryptedDocument(
        val path: String,
        val iv: String
    )

    private companion object {

        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        const val KEY_ALIAS =
            "secure_vault_master_key"

        const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        const val GCM_TAG_LENGTH =
            128

        const val GCM_IV_LENGTH =
            12

        const val PREVIEW_RETENTION_MILLIS =
            10 * 60 * 1000L
    }
}