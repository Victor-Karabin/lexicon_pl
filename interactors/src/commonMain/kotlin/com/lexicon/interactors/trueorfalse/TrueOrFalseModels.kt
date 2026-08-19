package com.lexicon.interactors.trueorfalse

data class TrueOrFalseStepResponse(
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val word: String,
    val displayedTranslation: String,
    val isDisplayedTranslationCorrect: Boolean,
)

data class TrueOrFalseSessionResponse(
    val sessionId: String,
    val steps: List<TrueOrFalseStepResponse>,
)
