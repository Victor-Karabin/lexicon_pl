package com.lexicon.interactors.dictation

enum class DictationStepOutcome { CORRECT, INCORRECT, SKIPPED }

data class DictationStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val translationText: String,
)

data class DictationSessionResponse(
    val sessionId: String,
    val steps: List<DictationStepResponse>,
)
