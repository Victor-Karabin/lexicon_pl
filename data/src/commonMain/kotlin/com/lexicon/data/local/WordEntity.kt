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
    val isFavourite: Boolean = false,
    val searchKey: String = "",
    val cefr: String = "",
    val isDeleted: Boolean = false,
    /**
     * Written by the learner rather than shipped in the seed asset. The seed diff
     * would otherwise read such a word as one the asset had dropped and delete it,
     * so [VocabularySeeder] leaves these alone.
     */
    val isUserCreated: Boolean = false,
)

/**
 * Ids for hand-added words, counting down from -1.
 *
 * The asset numbers its words from 1 upwards and the corpus only grows, so no
 * negative id can ever collide with one a future release ships. That matters
 * because preset membership, lesson links and training history all key off this
 * id: a collision would silently re-point them at a different word.
 */
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
        isFavourite = isFavourite,
        cefr = cefr.ifEmpty { null },
    )
