package com.talkbridge.livetranslator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "TranscriptionItems")
data class TranscriptionItem (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val date: LocalDate?,
    val content: String
)