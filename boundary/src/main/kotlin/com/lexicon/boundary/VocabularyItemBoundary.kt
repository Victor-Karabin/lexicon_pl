package com.lexicon.boundary

data class VocabularyItemBoundary(
    val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val isFavourite: Boolean = false,
    /** CEFR band as written in the data, or null when the source did not state one. */
    val cefr: String? = null,
)
