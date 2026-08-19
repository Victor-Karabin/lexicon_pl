package com.lexicon.interactors.pronunciation

import com.lexicon.interactors.training.StepOutcome

data class SubmitPronunciationResultRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val recognizedText: String,
    val confidence: Float?,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitPronunciationResultResponse(
    val outcome: StepOutcome,
    val expectedText: String,
)

interface SubmitPronunciationResultUseCase {
    suspend operator fun invoke(request: SubmitPronunciationResultRequest): SubmitPronunciationResultResponse
}
