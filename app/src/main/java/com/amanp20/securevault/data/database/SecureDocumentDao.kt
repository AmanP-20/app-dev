package com.amanp20.securevault.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface SecureDocumentDao {
    @Query("SELECT COUNT(*) FROM secure_documents")
    fun observeDocumentCount(): LiveData<Int>
}
