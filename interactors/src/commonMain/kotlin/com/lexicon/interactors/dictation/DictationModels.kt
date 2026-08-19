package com.lexicon.interactors.dictation

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
