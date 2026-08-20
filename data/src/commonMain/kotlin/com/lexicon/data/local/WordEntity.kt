package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lexicon.common.foldForSearch
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word

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

fun WordEntity.toWord(): Word =
    Word(
        id = VocabularyId(id),
        text = text,
        translation = translation,
        transcription = transcription,
        isInStudySet = isInStudySet,
        cefr = CefrLevel.ofName(cefr.ifEmpty { null }),
    )
