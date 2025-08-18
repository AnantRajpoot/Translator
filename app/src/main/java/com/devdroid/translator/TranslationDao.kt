package com.devdroid.translator

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TranslationDao {
    @Insert
    suspend fun insert(item: TranslationItem)

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<TranslationItem>

    //THIS FUNCTION to delete a single item
    @Query("DELETE FROM translation_history WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("DELETE FROM translation_history")
    suspend fun clearAll()
}