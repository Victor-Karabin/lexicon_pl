package com.lexicon.pl.domain.dictation

data class Word(
    val id: Long,
    val text: String,
    val translation: String,
    val transcription: String,
)
