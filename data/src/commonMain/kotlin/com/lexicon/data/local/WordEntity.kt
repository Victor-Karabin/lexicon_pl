package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.common.foldForSearch

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val isInStudySet: Boolean = false,
    val searchKey: String = "",
    val cefr: String = "",
    val isDeleted: Boolean = false,
    val isUserCreated: Boolean = false,
)

fun nextUserWordId(lowestExistingId: Long?): Long = minOf(lowestExistingId ?: 0L, 0L) - 1

fun searchKeyFor(
    text: String,
    translation: String,
): String = "${text.foldForSearch()} ${translation.foldForSearch()}"

fun WordEntity.toBoundary(): VocabularyItemBoundary =
    VocabularyItemBoundary(
        id = id,
        text = text,
        translation = translation,
        transcription = transcription,
        isInStudySet = isInStudySet,
        cefr = cefr.ifEmpty { null },
    )
