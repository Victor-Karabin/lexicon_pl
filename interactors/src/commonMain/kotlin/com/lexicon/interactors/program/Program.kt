package com.lexicon.interactors.program

import com.lexicon.model.vocabulary.LocalizedText

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
    val isUserCreated: Boolean = true,
)

enum class ProgramDifficulty { BEGINNER, INTERMEDIATE, ADVANCED }

enum class ProgramVisibility { PUBLIC, PRIVATE }
