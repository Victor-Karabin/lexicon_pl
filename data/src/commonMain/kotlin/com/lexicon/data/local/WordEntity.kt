package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lexicon.common.foldForSearch
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus

@Entity(tableName = "words", indices = [Index("status")])
data class WordEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val status: String = WordStatus.UNDEFINED.name,
    val searchKey: String = "",
    val cefr: String = "",
    val example: String = "",
    val isDeleted: Boolean = false,
    val isUserCreated: Boolean = false,
    val picture: String? = null,
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
        status = WordStatus.ofName(status),
        cefr = CefrLevel.ofName(cefr.ifEmpty { null }),
        example = example,
        picture = picture,
    )
