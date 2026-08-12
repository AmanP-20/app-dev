package com.amanp20.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_documents")
data class SecureDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val mimeType: String,
    val isLocked: Boolean = false,
    val passwordHash: String? = null,
    val biometricProtected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
