package com.lexicon.interactors.dictation

import com.lexicon.model.training.StepOutcome

data class SubmitDictationAnswerRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedText: String,
    val submittedText: String,
    val tipUsed: Boolean,
    val skipped: Boolean,
)

data class SubmitDictationAnswerResponse(
    val outcome: StepOutcome,
    val expectedText: String,
)

interface SubmitDictationAnswerUseCase {
    suspend operator fun invoke(request: SubmitDictationAnswerRequest): SubmitDictationAnswerResponse
}
