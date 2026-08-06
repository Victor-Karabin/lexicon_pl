package com.lexicon.interactors.dictation

enum class DictationStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class DictationStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    /** Text to synthesize via TTS and to validate the submitted answer against. */
    val expectedText: String,
    /** Base-language meaning, shown as the Tip hint (never the same as [expectedText]). */
    val translationText: String,
)

data class DictationSessionResponse(
    val sessionId: String,
    val steps: List<DictationStepResponse>,
)
