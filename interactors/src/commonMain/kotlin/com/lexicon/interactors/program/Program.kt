package com.lexicon.interactors.program

import com.lexicon.interactors.presets.LocalizedText

/** A program as it is listed: what it is, plus everything it is configured to do. */
data class Program(
    val id: ProgramId,
    val level: String,
    val order: Int,
    val title: LocalizedText,
    val description: LocalizedText,
    val difficulty: ProgramDifficulty,
    val estimatedDays: Int,
    val visibility: ProgramVisibility,
    val config: ProgramConfig,
)

enum class ProgramDifficulty { BEGINNER, INTERMEDIATE, ADVANCED }

enum class ProgramVisibility { PUBLIC, PRIVATE }
