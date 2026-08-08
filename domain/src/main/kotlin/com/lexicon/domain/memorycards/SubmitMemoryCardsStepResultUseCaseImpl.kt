package com.lexicon.domain.memorycards

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.memorycards.MemoryCardsStepOutcome
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultRequest
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultResponse
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
import javax.inject.Inject

private const val TRAINING_TYPE_MEMORY_CARDS = "MEMORY_CARDS"

class SubmitMemoryCardsStepResultUseCaseImpl
    @Inject
    constructor(
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

        private fun resolveOutcome(request: SubmitMemoryCardsStepResultRequest): MemoryCardsStepOutcome =
            when {
                request.skipped -> MemoryCardsStepOutcome.SKIPPED
                request.incorrectAttempts == 0 -> MemoryCardsStepOutcome.CORRECT
                else -> MemoryCardsStepOutcome.INCORRECT
            }

        private fun MemoryCardsStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                MemoryCardsStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                MemoryCardsStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                MemoryCardsStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
