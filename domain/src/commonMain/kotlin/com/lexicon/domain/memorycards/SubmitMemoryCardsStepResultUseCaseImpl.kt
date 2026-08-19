package com.lexicon.domain.memorycards

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.training.toBoundary
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultRequest
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultResponse
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
import com.lexicon.interactors.training.StepOutcome

private const val TRAINING_TYPE_MEMORY_CARDS = "MEMORY_CARDS"

class SubmitMemoryCardsStepResultUseCaseImpl(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val clock: Clock,
) : SubmitMemoryCardsStepResultUseCase {
    override suspend fun invoke(request: SubmitMemoryCardsStepResultRequest): SubmitMemoryCardsStepResultResponse {
        val outcome = resolveOutcome(request)
        val completedAt = clock.nowEpochMillis()

        request.vocabularyItemIds.forEach { vocabularyItemId ->
            trainingHistoryRepository.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_MEMORY_CARDS,
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

        return SubmitMemoryCardsStepResultResponse(outcome = outcome)
    }

    private fun resolveOutcome(request: SubmitMemoryCardsStepResultRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            request.incorrectAttempts == 0 -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
