package com.lexicon.interactors.dictationpuzzle

import com.lexicon.model.training.StepOutcome

data class SubmitDictationPuzzleAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val submittedText: String,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitDictationPuzzleAnswerResponse(
    val outcome: StepOutcome,
    val expectedText: String,
)

interface SubmitDictationPuzzleAnswerUseCase {
    suspend operator fun invoke(request: SubmitDictationPuzzleAnswerRequest): SubmitDictationPuzzleAnswerResponse
}
