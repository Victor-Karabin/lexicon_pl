package com.lexicon.interactors.program

import com.lexicon.model.training.TrainingType

data class ProgramDraft(
    val title: String,
    val description: String = "",
    val newWordsPerDay: Int,
    val reviewWordsPerDay: Int,
    val trainings: List<String> = emptyList(),
)

val defaultProgramQueue: List<String> =
    listOf(
        TrainingType.WORD_CARD,
        TrainingType.IMAGE_TEST,
        TrainingType.WORD_MATCH,
        TrainingType.TRUE_OR_FALSE,
        TrainingType.PUZZLE,
        TrainingType.DICTATION_PUZZLE,
        TrainingType.DICTATION,
        TrainingType.PASSAGE_BANK,
        TrainingType.PASSAGE_WRITE,
        TrainingType.PRONUNCIATION_CHECK,
        TrainingType.PRONUNCIATION_SENTENCES,
        TrainingType.FILLWORD,
        TrainingType.MEMORY_CARDS,
        TrainingType.CROSSWORD,
    ).map { it.id }
