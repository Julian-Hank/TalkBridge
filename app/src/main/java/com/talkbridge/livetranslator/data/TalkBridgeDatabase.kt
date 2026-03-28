package com.talkbridge.livetranslator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.talkbridge.livetranslator.data.local.dao.TranscriptionItemDao
import com.talkbridge.livetranslator.data.local.dao.TranslationHistoryItemDao
import com.talkbridge.livetranslator.data.local.entity.LocalDateConverter
import com.talkbridge.livetranslator.data.local.entity.TranscriptionItem
import com.talkbridge.livetranslator.data.local.entity.TranslationHistoryItem

@TypeConverters(LocalDateConverter::class)
@Database(entities = [TranslationHistoryItem::class, TranscriptionItem::class], version = 1, exportSchema = false)
abstract class TalkBridgeDatabase: RoomDatabase() {

    abstract fun translationHistoryItemDao(): TranslationHistoryItemDao
    abstract fun transcriptionItemDao(): TranscriptionItemDao

    companion object {
        @Volatile
        private var Instance: TalkBridgeDatabase? = null
        fun getDatabase(context: Context): TalkBridgeDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, TalkBridgeDatabase::class.java, "talkBridge_database")
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}