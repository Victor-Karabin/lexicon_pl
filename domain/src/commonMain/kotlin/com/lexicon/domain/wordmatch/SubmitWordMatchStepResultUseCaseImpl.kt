package com.lexicon.domain.wordmatch

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.training.toBoundary
import com.lexicon.interactors.training.StepOutcome
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultRequest
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultResponse
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase

private const val TRAINING_TYPE_WORD_MATCH = "WORD_MATCH"

class SubmitWordMatchStepResultUseCaseImpl(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val clock: Clock,
) : SubmitWordMatchStepResultUseCase {
    override suspend fun invoke(request: SubmitWordMatchStepResultRequest): SubmitWordMatchStepResultResponse {
        val outcome = resolveOutcome(request)
        val completedAt = clock.nowEpochMillis()

        request.vocabularyItemIds.forEach { vocabularyItemId ->
            trainingHistoryRepository.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_WORD_MATCH,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = vocabularyItemId,
                    expectedAnswer = "matched",
                    submittedAnswer = request.incorrectAttempts.toString(),
                    outcome = outcome.toBoundary(),
                    tipUsed = false,
                    completedAtEpochMillis = completedAt,
                ),
            )
        }

        return SubmitWordMatchStepResultResponse(outcome = outcome)
    }

    private fun resolveOutcome(request: SubmitWordMatchStepResultRequest): StepOutcome =
        if (request.incorrectAttempts == 0) StepOutcome.CORRECT else StepOutcome.INCORRECT
}
