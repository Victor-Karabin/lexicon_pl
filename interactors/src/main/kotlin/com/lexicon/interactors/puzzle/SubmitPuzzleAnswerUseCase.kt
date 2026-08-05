package com.lexicon.interactors.puzzle

data class SubmitPuzzleAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    /** Empty when the step was skipped rather than answered. */
    val submittedText: String,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitPuzzleAnswerResponse(
    val outcome: PuzzleStepOutcome,
    val expectedText: String,
)

interface SubmitPuzzleAnswerUseCase {
    suspend operator fun invoke(request: SubmitPuzzleAnswerRequest): SubmitPuzzleAnswerResponse
}
