package com.amanp20.securevault.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

object FileMetadataUtils {

    data class FileMetadata(
        val name: String,
        val mimeType: String,
        val size: Long,
        val category: String,
        val lastModified: Long
    )

    fun readMetadata(
        context: Context,
        uri: Uri
    ): FileMetadata {

        val resolver =
            context.contentResolver

        val mimeType =
            resolver.getType(uri)
                ?: "application/octet-stream"

        var name: String? = null
        var size = 0L

        resolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val nameIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                val sizeIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.SIZE
                    )

                if (nameIndex >= 0) {
                    name =
                        cursor.getString(
                            nameIndex
                        )
                }

                if (sizeIndex >= 0 &&
                    !cursor.isNull(sizeIndex)
                ) {
                    size =
                        cursor.getLong(
                            sizeIndex
                        )
                }
            }
        }

        if (name.isNullOrBlank()) {

            name =
                uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.ifBlank { null }
        }

        if (name.isNullOrBlank()) {

            val extension =
                MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(
                        mimeType
                    )

            name =
                if (extension.isNullOrBlank()) {
                    "document"
                } else {
                    "document.$extension"
                }
        }

        if (size <= 0L) {

            runCatching {

                resolver
                    .openAssetFileDescriptor(
                        uri,
                        "r"
                    )
                    ?.use {
                        size =
                            it.length
                    }
            }
        }

        return FileMetadata(
            name = sanitizeName(name),
            mimeType = mimeType,
            size = size.coerceAtLeast(0L),
            category = categoryFor(mimeType),
            lastModified = System.currentTimeMillis()
        )
    }

    fun categoryFor(
        mimeType: String
    ): String {

        return when {

            mimeType.startsWith(
                "image/"
            ) -> "Images"

            mimeType.startsWith(
                "video/"
            ) -> "Videos"

            mimeType.startsWith(
                "audio/"
            ) -> "Audio"

            mimeType == "application/pdf" ||
                mimeType.startsWith(
                    "text/"
                ) ||
                mimeType == "application/msword" ||
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                mimeType == "application/vnd.ms-excel" ||
                mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                mimeType == "application/vnd.ms-powerpoint" ||
                mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> {
                "Documents"
            }

            else -> "Other"
        }
    }

    private fun sanitizeName(
        name: String
    ): String {

        return name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            )
            .trim()
            .take(200)
            .ifBlank {
                "document"
            }
    }
}