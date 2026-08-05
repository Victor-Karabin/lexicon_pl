package com.lexicon.pl.interactors.wordmatch

data class SubmitWordMatchStepResultRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemIds: List<Long>,
    val incorrectAttempts: Int,
    val skipped: Boolean,
)

data class SubmitWordMatchStepResultResponse(
    val outcome: WordMatchStepOutcome,
)

interface SubmitWordMatchStepResultUseCase {
    suspend operator fun invoke(request: SubmitWordMatchStepResultRequest): SubmitWordMatchStepResultResponse
}
