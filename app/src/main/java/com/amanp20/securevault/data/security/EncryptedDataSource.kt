package com.amanp20.securevault.data.security

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.File
import java.io.IOException
import javax.crypto.CipherInputStream

class EncryptedDataSource(
    private val securityManager: DocumentSecurityManager,
    private val encryptedPath: String,
    private val encodedIv: String
) : DataSource {

    private var inputStream: CipherInputStream? = null
    private var openedUri: Uri? = null

    override fun addTransferListener(
        transferListener: TransferListener
    ) {
    }

    override fun open(dataSpec: DataSpec): Long {
        if (dataSpec.position != 0L) {
            throw IOException("Encrypted video seeking is not supported")
        }

        val file = File(encryptedPath)

        if (!file.isFile) {
            throw IOException("Encrypted file does not exist")
        }

        inputStream =
            securityManager.openDecryptedStream(
                encryptedPath = encryptedPath,
                encodedIv = encodedIv
            )

        openedUri = dataSpec.uri

        return if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            file.length()
        }
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        if (length == 0) {
            return 0
        }

        val input =
            inputStream
                ?: throw IOException("DataSource is not open")

        return input.read(
            buffer,
            offset,
            length
        )
    }

    override fun getUri(): Uri? = openedUri

    override fun close() {
        runCatching {
            inputStream?.close()
        }

        inputStream = null
        openedUri = null
    }

    class Factory(
        private val securityManager: DocumentSecurityManager,
        private val encryptedPath: String,
        private val encodedIv: String
    ) : DataSource.Factory {

        override fun createDataSource(): DataSource {
            return EncryptedDataSource(
                securityManager = securityManager,
                encryptedPath = encryptedPath,
                encodedIv = encodedIv
            )
        }
    }
}