package com.lexicon.boundary

data class VocabularyItemBoundary(
    val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val isFavourite: Boolean = false,
)
