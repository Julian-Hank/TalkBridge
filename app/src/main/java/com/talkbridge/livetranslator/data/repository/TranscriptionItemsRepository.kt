package com.talkbridge.livetranslator.data.repository

import com.talkbridge.livetranslator.data.local.dao.TranscriptionItemDao
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import kotlinx.coroutines.flow.Flow


class TranscriptionItemsRepository(private val itemDao: TranscriptionItemDao) {
    fun getAllItemsStream(): Flow<List<TranscriptionItem>> = itemDao.getAllItems()

    fun getItemStream(id: Long): Flow<TranscriptionItem?> = itemDao.getItem(id)

    suspend fun getItem(id: Long): TranscriptionItem? = itemDao.getItemById(id)

    suspend fun insertItem(item: TranscriptionItem): Long {
        return itemDao.insert(item)
    }

    suspend fun deleteItem(item: TranscriptionItem) = itemDao.delete(item)
}