package com.lexicon.interactors.imagetest

import com.lexicon.interactors.training.StepOutcome

data class SubmitImageTestAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val correctOption: String,
    val selectedOption: String?,
    val skipped: Boolean,
)

data class SubmitImageTestAnswerResponse(
    val outcome: StepOutcome,
    val correctOption: String,
)

interface SubmitImageTestAnswerUseCase {
    suspend operator fun invoke(request: SubmitImageTestAnswerRequest): SubmitImageTestAnswerResponse
}
