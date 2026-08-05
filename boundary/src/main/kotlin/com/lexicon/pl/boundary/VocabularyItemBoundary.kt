package com.lexicon.pl.boundary

data class VocabularyItemBoundary(
    val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
)
