package com.talkbridge.livetranslator.data.repository

import com.talkbridge.livetranslator.data.local.dao.TranslationHistoryItemDao
import com.talkbridge.livetranslator.data.local.entity.TranslationHistoryItem
import kotlinx.coroutines.flow.Flow

class TranslationHistoryItemsRepository(private val itemDao: TranslationHistoryItemDao) {
    fun getAllItemsStream(): Flow<List<TranslationHistoryItem>> = itemDao.getAllItems()

    fun getItemStream(id: Int): Flow<TranslationHistoryItem?> = itemDao.getItem(id)

    suspend fun insertItem(item: TranslationHistoryItem) = itemDao.insert(item)

    suspend fun deleteItem(item: TranslationHistoryItem) = itemDao.delete(item)
}