package com.lexicon.interactors.program

data class ProgramDraft(
    val title: String,
    val description: String = "",
    val newWordsPerDay: Int,
    val reviewWordsPerDay: Int,
    val trainings: List<String> = emptyList(),
)
