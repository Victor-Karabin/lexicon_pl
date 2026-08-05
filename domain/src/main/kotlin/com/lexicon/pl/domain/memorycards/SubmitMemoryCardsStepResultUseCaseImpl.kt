package com.lexicon.pl.domain.memorycards

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.boundary.TrainingResultBoundary
import com.lexicon.pl.boundary.TrainingResultOutcomeBoundary
import com.lexicon.pl.common.Clock
import com.lexicon.pl.interactors.memorycards.MemoryCardsStepOutcome
import com.lexicon.pl.interactors.memorycards.SubmitMemoryCardsStepResultRequest
import com.lexicon.pl.interactors.memorycards.SubmitMemoryCardsStepResultResponse
import com.lexicon.pl.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
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

            // Result Recording lists vocabulary item identifiers (plural) per step; record one row
            // per pair, all sharing the step's outcome, same shape as Word Match.
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

        // This training has no Tip action. A step that was fully matched, even with wrong attempts
        // along the way, still completes — but the recorded result is Incorrect if any attempt was wrong.
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
