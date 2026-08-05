package com.lexicon.interactors.memorycards

data class SubmitMemoryCardsStepResultRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemIds: List<Long>,
    val incorrectAttempts: Int,
    val skipped: Boolean,
)

data class SubmitMemoryCardsStepResultResponse(
    val outcome: MemoryCardsStepOutcome,
)

interface SubmitMemoryCardsStepResultUseCase {
    suspend operator fun invoke(request: SubmitMemoryCardsStepResultRequest): SubmitMemoryCardsStepResultResponse
}
