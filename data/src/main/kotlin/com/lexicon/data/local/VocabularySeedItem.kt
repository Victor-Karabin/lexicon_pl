package com.lexicon.data.local

import kotlinx.serialization.Serializable

@Serializable
data class VocabularySeedItem(
    val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    /** Absent in the original mock asset, so it defaults rather than failing to parse. */
    val cefr: String = "",
)

fun VocabularySeedItem.toEntity(): WordEntity =
    WordEntity(
        id = id,
        text = text,
        translation = translation,
        transcription = transcription,
        searchKey = searchKeyFor(text, translation),
        cefr = cefr,
    )
