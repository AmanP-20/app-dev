package com.amanp20.securevault.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.amanp20.securevault.data.model.SecureDocument

@Dao
interface SecureDocumentDao {
    @Insert
    suspend fun insert(document: SecureDocument): Long

    @Query("SELECT * FROM secure_documents ORDER BY dateAdded DESC")
    fun observeAll(): LiveData<List<SecureDocument>>

    @Query("SELECT * FROM secure_documents WHERE id = :id")
    suspend fun getById(id: Long): SecureDocument?

    @Query("SELECT * FROM secure_documents WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    fun search(query: String): LiveData<List<SecureDocument>>

    @Query("SELECT * FROM secure_documents WHERE category = :category ORDER BY dateAdded DESC")
    fun observeByCategory(category: String): LiveData<List<SecureDocument>>

    @Query("SELECT EXISTS(SELECT 1 FROM secure_documents WHERE uri = :uri)")
    suspend fun existsByUri(uri: String): Boolean

    @Query("DELETE FROM secure_documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @androidx.room.Update
    suspend fun update(document: SecureDocument)

    @Query("SELECT COUNT(*) FROM secure_documents")
    fun observeDocumentCount(): LiveData<Int>
}
