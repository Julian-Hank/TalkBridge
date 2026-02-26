package com.talkbridge.livetranslator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Date

@Entity(tableName = "TranscriptionItems")
data class TranscriptionItem (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val date: Date,
    val content: String
)