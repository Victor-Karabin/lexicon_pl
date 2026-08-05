package com.lexicon.interactors.imagetest

data class SubmitImageTestAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val correctOption: String,
    /** Null when the step was skipped rather than answered. */
    val selectedOption: String?,
    val skipped: Boolean,
)

data class SubmitImageTestAnswerResponse(
    val outcome: ImageTestStepOutcome,
    val correctOption: String,
)

/** This training has no Tip action per spec. */
interface SubmitImageTestAnswerUseCase {
    suspend operator fun invoke(request: SubmitImageTestAnswerRequest): SubmitImageTestAnswerResponse
}
