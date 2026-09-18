package com.amanp20.securevault.data.security

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Owns document encryption and keeps encrypted bytes inside the application's private files.
 * The key never leaves Android Keystore.
 */
class DocumentSecurityManager(context: Context) {
    private val appContext = context.applicationContext
    private val storageDir = File(appContext.filesDir, "protected_documents").apply { mkdirs() }
    private val cacheDir = File(appContext.cacheDir, "document_previews").apply { mkdirs() }

    fun encryptDocument(documentId: Long, source: Uri): EncryptedDocument {
        val destination = File(storageDir, "$documentId.enc")
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val cipher = cipher(Cipher.ENCRYPT_MODE, key(), iv)
        try {
            val input = appContext.contentResolver.openInputStream(source)
                ?: throw IOException("Unable to read document")
            input.use { stream ->
                destination.outputStream().use { output ->
                    CipherOutputStream(output, cipher).use { encrypted -> stream.copyTo(encrypted) }
                }
            }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
        return EncryptedDocument(destination.absolutePath, Base64.encodeToString(iv, Base64.NO_WRAP))
    }

    fun decryptDocument(documentId: Long, encryptedPath: String, encodedIv: String): Uri {
        val source = File(encryptedPath)
        require(source.isFile) { "Protected document is missing" }
        val output = File(cacheDir, "$documentId-${System.currentTimeMillis()}.bin")
        val cipher = cipher(
            Cipher.DECRYPT_MODE,
            key(),
            Base64.decode(encodedIv, Base64.NO_WRAP)
        )
        source.inputStream().use { input ->
            CipherInputStream(input, cipher).use { encrypted ->
                output.outputStream().use { decrypted -> encrypted.copyTo(decrypted) }
            }
        }
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            output
        )
    }

    fun removeProtection(encryptedPath: String?) {
        encryptedPath?.let { File(it).delete() }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(false)
                    .build()
            )
            generateKey()
        }
    }

    private fun cipher(mode: Int, key: SecretKey, iv: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }

    data class EncryptedDocument(val path: String, val iv: String)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "secure_vault_document_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
        const val GCM_IV_LENGTH = 12
    }
}
