package com.lexicon.interactors.trueorfalse

import com.lexicon.model.training.StepOutcome

data class SubmitTrueOrFalseAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val isDisplayedTranslationCorrect: Boolean,
    val userAnsweredTrue: Boolean,
)

data class SubmitTrueOrFalseAnswerResponse(
    val outcome: StepOutcome,
    val isDisplayedTranslationCorrect: Boolean,
)

interface SubmitTrueOrFalseAnswerUseCase {
    suspend operator fun invoke(request: SubmitTrueOrFalseAnswerRequest): SubmitTrueOrFalseAnswerResponse
}
