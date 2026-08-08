package com.lexicon.app.navigation

import com.lexicon.presentation.main.TrainingIds

internal object LexiconDestinations {
    const val SPLASH = "splash"
    const val MAIN = "main"

    const val DICTATION = TrainingIds.DICTATION
    const val DICTATION_PUZZLE = TrainingIds.DICTATION_PUZZLE
    const val TRUE_OR_FALSE = TrainingIds.TRUE_OR_FALSE
    const val WORD_MATCH = TrainingIds.WORD_MATCH
    const val PRONUNCIATION_CHECK = TrainingIds.PRONUNCIATION_CHECK
    const val PUZZLE = TrainingIds.PUZZLE
    const val IMAGE_TEST = TrainingIds.IMAGE_TEST
    const val MEMORY_CARDS = TrainingIds.MEMORY_CARDS
    const val MIX = TrainingIds.MIX
    const val CROSSWORD = TrainingIds.CROSSWORD

    const val PRESET_DETAIL = "preset/{presetId}"

    fun presetDetail(presetId: String) = "preset/$presetId"

    const val SESSION_RESULT = "session_result/{correct}/{incorrect}/{skipped}/{tipsUsed}"

    fun sessionResult(
        correct: Int,
        incorrect: Int,
        skipped: Int,
        tipsUsed: Int,
    ) = "session_result/$correct/$incorrect/$skipped/$tipsUsed"
}
