package com.lexicon.pl.interactors.wordbuilder

enum class WordBuilderStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class WordBuilderStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    /** Target spelling to reconstruct from letter tiles; never shown directly. */
    val expectedText: String,
    /** Definition/translation shown as the clue. */
    val clueText: String,
)

data class WordBuilderSessionResponse(
    val sessionId: String,
    val steps: List<WordBuilderStepResponse>,
)
