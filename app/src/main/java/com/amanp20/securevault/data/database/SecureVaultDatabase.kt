package com.amanp20.securevault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.data.model.SecureNote

@Database(
    entities = [SecureDocument::class, SecureNote::class],
    version = 1,
    exportSchema = false
)
abstract class SecureVaultDatabase : RoomDatabase() {

    abstract fun secureDocumentDao(): SecureDocumentDao

    abstract fun secureNoteDao(): SecureNoteDao

    companion object {
        @Volatile
        private var instance: SecureVaultDatabase? = null

        fun getInstance(context: Context): SecureVaultDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SecureVaultDatabase::class.java,
                    "secure_vault.db"
                ).build().also { instance = it }
            }
        }
    }
}
