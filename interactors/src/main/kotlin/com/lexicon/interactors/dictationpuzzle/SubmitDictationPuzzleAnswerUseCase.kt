package com.lexicon.interactors.dictationpuzzle

data class SubmitDictationPuzzleAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    /** Empty when the step was skipped rather than answered. */
    val submittedText: String,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitDictationPuzzleAnswerResponse(
    val outcome: DictationPuzzleStepOutcome,
    val expectedText: String,
)

interface SubmitDictationPuzzleAnswerUseCase {
    suspend operator fun invoke(request: SubmitDictationPuzzleAnswerRequest): SubmitDictationPuzzleAnswerResponse
}
