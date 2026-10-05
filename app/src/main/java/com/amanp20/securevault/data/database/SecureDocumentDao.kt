package com.amanp20.securevault.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.amanp20.securevault.data.model.SecureDocument

@Dao
interface SecureDocumentDao {

    @Insert
    suspend fun insert(document: SecureDocument): Long

    @Update
    suspend fun update(document: SecureDocument)

    @Delete
    suspend fun delete(document: SecureDocument)

    @Query("""
        SELECT * FROM secure_documents
        ORDER BY dateAdded DESC
    """)
    fun observeAll(): LiveData<List<SecureDocument>>

    @Query("""
        SELECT * FROM secure_documents
        WHERE category = :category
        ORDER BY dateAdded DESC
    """)
    fun observeByCategory(
        category: String
    ): LiveData<List<SecureDocument>>

    @Query("""
        SELECT * FROM secure_documents
        WHERE originalName LIKE '%' || :query || '%'
        ORDER BY dateAdded DESC
    """)
    fun search(
        query: String
    ): LiveData<List<SecureDocument>>

    @Query("""
        SELECT * FROM secure_documents
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getById(
        id: Long
    ): SecureDocument?

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM secure_documents
            WHERE originalName = :name
        )
    """)
    suspend fun existsByName(
        name: String
    ): Boolean

    @Query("""
        SELECT COUNT(*)
        FROM secure_documents
    """)
    fun observeDocumentCount(): LiveData<Int>

    @Query("""
        SELECT COALESCE(SUM(fileSize), 0)
        FROM secure_documents
    """)
    fun observeTotalSize(): LiveData<Long>
}