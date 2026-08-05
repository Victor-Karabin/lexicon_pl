package com.lexicon.pl.app.navigation

import com.lexicon.pl.presentation.main.TrainingIds

internal object LexiconDestinations {
    const val SPLASH = "splash"
    const val MAIN = "main"

    // Route strings match TrainingIds exactly, since TrainingsScreen navigates by passing that id up.
    const val DICTATION = TrainingIds.DICTATION
    const val DICTATION_PUZZLE = TrainingIds.DICTATION_PUZZLE
    const val WORD_BUILDER = TrainingIds.WORD_BUILDER
    const val TRUE_OR_FALSE = TrainingIds.TRUE_OR_FALSE
    const val WORD_MATCH = TrainingIds.WORD_MATCH
    const val PRONUNCIATION_CHECK = TrainingIds.PRONUNCIATION_CHECK
    const val PUZZLE = TrainingIds.PUZZLE
    const val IMAGE_TEST = TrainingIds.IMAGE_TEST
    const val MEMORY_CARDS = TrainingIds.MEMORY_CARDS

    const val SESSION_RESULT = "session_result/{correct}/{incorrect}/{skipped}"

    fun sessionResult(
        correct: Int,
        incorrect: Int,
        skipped: Int,
    ) = "session_result/$correct/$incorrect/$skipped"
}
