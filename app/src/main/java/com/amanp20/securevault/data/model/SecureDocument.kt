package com.amanp20.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_documents")
data class SecureDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val uri: String,
    val mimeType: String,
    val fileSize: Long,
    val category: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastModified: Long? = null,
    val isLocked: Boolean = false,
    val passwordHash: String? = null,
    val biometricProtected: Boolean = false,
    val encryptedFilePath: String? = null,
    val encryptionIv: String? = null,
    val lockedAt: Long? = null,
    val lockType: String = "NONE",
    val passwordSalt: String? = null,
    val failedAttempts: Int = 0,
    val blockedUntil: Long? = null
)
