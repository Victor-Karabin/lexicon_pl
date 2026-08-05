package com.lexicon.pl.interactors.pronunciation

data class SubmitPronunciationResultRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    /** Empty when the step was skipped rather than answered. */
    val recognizedText: String,
    /** 0f-1f confidence from the recognizer, when available. */
    val confidence: Float?,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitPronunciationResultResponse(
    val outcome: PronunciationStepOutcome,
    val expectedText: String,
)

interface SubmitPronunciationResultUseCase {
    suspend operator fun invoke(request: SubmitPronunciationResultRequest): SubmitPronunciationResultResponse
}
