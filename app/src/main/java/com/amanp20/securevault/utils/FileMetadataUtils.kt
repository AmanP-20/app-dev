package com.amanp20.securevault.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import com.amanp20.securevault.data.model.SecureDocument

object FileMetadataUtils {
    private val supportedMimeTypes = setOf(
        "application/pdf", "text/plain", "image/jpeg", "image/png", "image/webp",
        "video/mp4", "audio/mpeg", "audio/wav"
    )

    fun readDocument(resolver: ContentResolver, uri: Uri): SecureDocument? {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: return null
        var size = 0L
        var lastModified: Long? = null
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null,
            null,
            null
        )
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        .takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
                    cursor.getColumnIndex(OpenableColumns.SIZE)
                        .takeIf { it >= 0 }?.let { size = cursor.getLong(it) }
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        .takeIf { it >= 0 }?.let { lastModified = cursor.getLong(it) }
                }
            }
        val mimeType = resolver.getType(uri) ?: return null
        if (!isSupported(mimeType)) return null
        return SecureDocument(
            name = name,
            uri = uri.toString(),
            mimeType = mimeType,
            fileSize = size,
            category = categoryFor(mimeType),
            lastModified = lastModified
        )
    }

    private fun isSupported(mimeType: String): Boolean =
        mimeType in supportedMimeTypes || mimeType.startsWith("image/") ||
            mimeType.startsWith("video/") || mimeType.startsWith("audio/") ||
            mimeType.startsWith("text/") || mimeType.contains("word") ||
            mimeType.contains("excel") || mimeType.contains("presentation")

    fun categoryFor(mimeType: String): String = when {
        mimeType == "application/pdf" -> "PDF"
        mimeType.startsWith("image/") -> "Image"
        mimeType.startsWith("video/") -> "Video"
        mimeType.startsWith("audio/") -> "Audio"
        mimeType.startsWith("text/") || mimeType.contains("word") ||
            mimeType.contains("excel") || mimeType.contains("presentation") -> "Document"
        else -> "Other"
    }
}
