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
    /** Marks a word for study. Trainings draw from these and nothing else. */
    val isFavourite: Boolean = false,
    /**
     * Both languages folded into one column, so a search is a single LIKE rather than a scan
     * that has to fold every row it looks at. Derived, never authored — see [searchKeyFor].
     */
    val searchKey: String = "",
    /** CEFR band, e.g. "A1". Empty for a row whose source did not state one. */
    val cefr: String = "",
)

/** The one place a word's search key is built, so stored keys and queries always agree. */
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
        isFavourite = isFavourite,
        cefr = cefr.ifEmpty { null },
    )
