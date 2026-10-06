package com.amanp20.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_documents")
data class SecureDocument(
    
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalName: String,
    val encryptedFilePath: String,
    val encryptionIv: String,
    val mimeType: String,
    val fileSize: Long,
    val category: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastModified: Long? = null
)