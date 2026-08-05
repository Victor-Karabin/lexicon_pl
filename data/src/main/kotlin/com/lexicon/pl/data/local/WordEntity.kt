package com.lexicon.pl.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
)
