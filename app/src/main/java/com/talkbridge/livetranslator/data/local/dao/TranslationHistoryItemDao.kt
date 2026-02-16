package com.talkbridge.livetranslator.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.talkbridge.livetranslator.data.local.entity.TranslationHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationHistoryItemDao {
    @Query("SELECT * from TranslationHistoryItems ORDER BY id DESC")
    fun getAllItems(): Flow<List<TranslationHistoryItem>>

    @Query("SELECT * from TranslationHistoryItems WHERE id = :id")
    fun getItem(id: Int): Flow<TranslationHistoryItem>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TranslationHistoryItem)

    @Delete
    suspend fun delete(item: TranslationHistoryItem)
}