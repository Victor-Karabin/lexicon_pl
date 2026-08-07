package com.lexicon.interactors.dictation

data class SubmitDictationAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    /** Empty when the step was skipped rather than answered. */
    val submittedText: String,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitDictationAnswerResponse(
    val outcome: DictationStepOutcome,
    val expectedText: String,
)

interface SubmitDictationAnswerUseCase {
    suspend operator fun invoke(request: SubmitDictationAnswerRequest): SubmitDictationAnswerResponse
}
