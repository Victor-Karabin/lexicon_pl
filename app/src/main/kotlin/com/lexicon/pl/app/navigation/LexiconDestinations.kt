package com.lexicon.pl.app.navigation

internal object LexiconDestinations {
    const val SPLASH = "splash"
    const val MAIN = "main"
    const val DICTATION = "dictation"
    const val DICTATION_RESULT = "dictation_result/{correct}/{incorrect}/{skipped}"

    fun dictationResult(
        correct: Int,
        incorrect: Int,
        skipped: Int,
    ) = "dictation_result/$correct/$incorrect/$skipped"
}
