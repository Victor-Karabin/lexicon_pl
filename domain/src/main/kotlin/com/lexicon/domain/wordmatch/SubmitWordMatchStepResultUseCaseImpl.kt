package com.lexicon.domain.wordmatch

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultRequest
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultResponse
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase
import com.lexicon.interactors.wordmatch.WordMatchStepOutcome
import javax.inject.Inject

private const val TRAINING_TYPE_WORD_MATCH = "WORD_MATCH"

class SubmitWordMatchStepResultUseCaseImpl
    @Inject
    constructor(
        private val trainingHistoryRepository: TrainingHistoryRepository,
        private val clock: Clock,
    ) : SubmitWordMatchStepResultUseCase {
        override suspend fun invoke(request: SubmitWordMatchStepResultRequest): SubmitWordMatchStepResultResponse {
            val outcome = resolveOutcome(request)
            val completedAt = clock.nowEpochMillis()

            // The spec's Result Recording lists vocabulary item identifiers (plural) per step;
            // record one row per pair, all sharing the step's outcome, reusing the existing single-item shape as-is.
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

        // This training has no Tip action. A step that was fully matched, even with wrong attempts along
        // the way, still completes — but the recorded result is Incorrect if any attempt was wrong.
        private fun resolveOutcome(request: SubmitWordMatchStepResultRequest): WordMatchStepOutcome =
            when {
                request.skipped -> WordMatchStepOutcome.SKIPPED
                request.incorrectAttempts == 0 -> WordMatchStepOutcome.CORRECT
                else -> WordMatchStepOutcome.INCORRECT
            }

        private fun WordMatchStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                WordMatchStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                WordMatchStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                WordMatchStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
