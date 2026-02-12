package com.talkbridge.livetranslator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TranscriptionItems")
class TranscriptionItem (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String
)