package com.talkbridge.livetranslator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.talkbridge.livetranslator.data.repository.TranscriptionItemsRepository
import com.talkbridge.livetranslator.data.repository.TranslationHistoryItemsRepository
import com.talkbridge.livetranslator.data.repository.UserPreferencesRepository

interface AppContainer {
    val transcriptionItemsRepository: TranscriptionItemsRepository
    val translationHistoryItemsRepository: TranslationHistoryItemsRepository
    val userPreferencesRepository: UserPreferencesRepository
    val talkBridgeClient: TalkBridgeClient

}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "talkBridge_preferences"
)

class AppDataContainer(private val context: Context) : AppContainer {
    override val transcriptionItemsRepository: TranscriptionItemsRepository by lazy {
        TranscriptionItemsRepository(TalkBridgeDatabase.getDatabase(context).transcriptionItemDao())
    }
    override val translationHistoryItemsRepository: TranslationHistoryItemsRepository by lazy {
        TranslationHistoryItemsRepository(TalkBridgeDatabase.getDatabase(context).translationHistoryItemDao())
    }
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.dataStore)
    }
    override val talkBridgeClient: TalkBridgeClient by lazy {
        TalkBridgeClient(context)
    }
}