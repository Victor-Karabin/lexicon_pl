package com.lexicon.interactors.trueorfalse

data class SubmitTrueOrFalseAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val isDisplayedTranslationCorrect: Boolean,
    val userAnsweredTrue: Boolean,
)

data class SubmitTrueOrFalseAnswerResponse(
    val outcome: TrueOrFalseStepOutcome,
    val isDisplayedTranslationCorrect: Boolean,
)

interface SubmitTrueOrFalseAnswerUseCase {
    suspend operator fun invoke(request: SubmitTrueOrFalseAnswerRequest): SubmitTrueOrFalseAnswerResponse
}
