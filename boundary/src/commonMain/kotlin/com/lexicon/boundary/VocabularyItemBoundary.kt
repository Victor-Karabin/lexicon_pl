package com.lexicon.boundary

data class VocabularyItemBoundary(
    val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
    val isInStudySet: Boolean = false,
    val cefr: String? = null,
)
