package com.amanp20.securevault.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface SecureNoteDao {
    @Query("SELECT COUNT(*) FROM secure_notes")
    fun observeNoteCount(): LiveData<Int>
}
