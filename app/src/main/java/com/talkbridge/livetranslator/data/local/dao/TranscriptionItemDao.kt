package com.talkbridge.livetranslator.data.local.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import kotlinx.coroutines.flow.Flow

interface TranscriptionItemDao {
    @Query("SELECT * from TranslationHistoryItems ORDER BY id DESC")
    fun getAllItems(): Flow<List<TranscriptionItem>>

    @Query("SELECT * from TranslationHistoryItems WHERE id = :id")
    fun getItem(id: Int): Flow<TranscriptionItem>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TranscriptionItem)

    @Delete
    suspend fun delete(item: TranscriptionItem)
}