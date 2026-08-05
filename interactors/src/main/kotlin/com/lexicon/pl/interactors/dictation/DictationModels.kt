package com.lexicon.pl.interactors.dictation

enum class DictationStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class DictationStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    /** Text to synthesize via TTS and to validate the submitted answer against. */
    val expectedText: String,
)

data class DictationSessionResponse(
    val sessionId: String,
    val steps: List<DictationStepResponse>,
)
