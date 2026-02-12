package com.talkbridge.livetranslator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TranslationHistoryItems")
data class TranslationHistoryItem (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val translatedContent: String,
    val initialContent: String
)