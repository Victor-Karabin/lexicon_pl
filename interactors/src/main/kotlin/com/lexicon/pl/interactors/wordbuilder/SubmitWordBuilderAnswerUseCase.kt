package com.lexicon.pl.interactors.wordbuilder

data class SubmitWordBuilderAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    /** Empty when the step was skipped rather than answered. */
    val submittedText: String,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitWordBuilderAnswerResponse(
    val outcome: WordBuilderStepOutcome,
    val expectedText: String,
)

interface SubmitWordBuilderAnswerUseCase {
    suspend operator fun invoke(request: SubmitWordBuilderAnswerRequest): SubmitWordBuilderAnswerResponse
}
