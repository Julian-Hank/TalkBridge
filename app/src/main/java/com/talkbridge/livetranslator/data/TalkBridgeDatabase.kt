package com.talkbridge.livetranslator.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import com.talkbridge.livetranslator.data.local.entity.TranslationHistoryItem

@Database(entities = [TranslationHistoryItem::class, TranscriptionItem::class], version = 1, exportSchema = false)
abstract class TalkBridgeDatabase: RoomDatabase() {

}