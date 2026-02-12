package com.talkbridge.livetranslator.data

import android.content.Context
import com.talkbridge.livetranslator.data.local.dao.TranscriptionItemDao
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import com.talkbridge.livetranslator.data.repository.TranscriptionItemsRepository
import com.talkbridge.livetranslator.data.repository.TranslationHistoryItemsRepository
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository

interface AppContainer {
    val transcriptionItemsRepository: TranscriptionItemsRepository
    val translationHistoryItemsRepository: TranslationHistoryItemsRepository
    val userPreferencesRepository: UserPreferencesRepository
}

//class AppDataContainer(private val context: Context) : AppContainer {
//    override val transcriptionItemsRepository: TranscriptionItemsRepository by lazy {
//        TranscriptionItemsRepository()
//    }
//}