package com.lexicon.interactors.wordmatch

import com.lexicon.interactors.training.StepOutcome

data class SubmitWordMatchStepResultRequest(
    val sessionId: String,
    val stepIndex: Int,
    val vocabularyItemIds: List<Long>,
    val incorrectAttempts: Int,
)

data class SubmitWordMatchStepResultResponse(
    val outcome: StepOutcome,
)

interface SubmitWordMatchStepResultUseCase {
    suspend operator fun invoke(request: SubmitWordMatchStepResultRequest): SubmitWordMatchStepResultResponse
}
