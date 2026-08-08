package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lexicon.boundary.VocabularyItemBoundary

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    /** Marks a word as chosen for study; see [WordDao.getRandomForStudy]. */
    val isFavourite: Boolean = false,
)

fun WordEntity.toBoundary(): VocabularyItemBoundary =
    VocabularyItemBoundary(
        id = id,
        text = text,
        translation = translation,
        transcription = transcription,
        isFavourite = isFavourite,
    )
