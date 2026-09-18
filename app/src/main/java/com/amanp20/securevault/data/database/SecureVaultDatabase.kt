package com.amanp20.securevault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.data.model.SecureNote

@Database(
    entities = [SecureDocument::class, SecureNote::class],
    version = 5,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE secure_documents_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        dateAdded INTEGER NOT NULL,
                        lastModified INTEGER,
                        isLocked INTEGER NOT NULL,
                        passwordHash TEXT,
                        biometricProtected INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO secure_documents_new
                    (id, name, uri, mimeType, fileSize, category, dateAdded, lastModified, isLocked, passwordHash, biometricProtected)
                    SELECT id, title, filePath, mimeType, 0, 'Other', createdAt, updatedAt, isLocked, passwordHash, biometricProtected
                    FROM secure_documents
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE secure_documents")
                database.execSQL("ALTER TABLE secure_documents_new RENAME TO secure_documents")
            }

        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE secure_documents ADD COLUMN encryptedFilePath TEXT")
                database.execSQL("ALTER TABLE secure_documents ADD COLUMN encryptionIv TEXT")
                database.execSQL("ALTER TABLE secure_documents ADD COLUMN lockedAt INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE secure_documents ADD COLUMN lockType TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE secure_documents ADD COLUMN passwordSalt TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE secure_documents ADD COLUMN failedAttempts INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE secure_documents ADD COLUMN blockedUntil INTEGER")
            }
        }
    }
}
