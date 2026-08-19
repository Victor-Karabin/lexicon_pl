package com.lexicon.interactors.puzzle

import com.lexicon.interactors.training.StepOutcome

data class SubmitPuzzleAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val submittedText: String,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitPuzzleAnswerResponse(
    val outcome: StepOutcome,
    val expectedText: String,
)

interface SubmitPuzzleAnswerUseCase {
    suspend operator fun invoke(request: SubmitPuzzleAnswerRequest): SubmitPuzzleAnswerResponse
}
