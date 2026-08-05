package com.lexicon.pl.domain.trueorfalse

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.boundary.TrainingResultBoundary
import com.lexicon.pl.boundary.TrainingResultOutcomeBoundary
import com.lexicon.pl.common.Clock
import com.lexicon.pl.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.pl.interactors.trueorfalse.SubmitTrueOrFalseAnswerResponse
import com.lexicon.pl.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.pl.interactors.trueorfalse.TrueOrFalseStepOutcome
import javax.inject.Inject

private const val TRAINING_TYPE_TRUE_OR_FALSE = "TRUE_OR_FALSE"

class SubmitTrueOrFalseAnswerUseCaseImpl
    @Inject
    constructor(
        private val trainingHistoryRepository: TrainingHistoryRepository,
        private val clock: Clock,
    ) : SubmitTrueOrFalseAnswerUseCase {
        override suspend fun invoke(request: SubmitTrueOrFalseAnswerRequest): SubmitTrueOrFalseAnswerResponse {
            val outcome = resolveOutcome(request)

            trainingHistoryRepository.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_TRUE_OR_FALSE,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = request.vocabularyItemId,
                    expectedAnswer = request.isDisplayedTranslationCorrect.toString(),
                    submittedAnswer = request.userAnsweredTrue?.toString().orEmpty(),
                    outcome = outcome.toBoundary(),
                    tipUsed = false,
                    completedAtEpochMillis = clock.nowEpochMillis(),
                ),
            )

            return SubmitTrueOrFalseAnswerResponse(
                outcome = outcome,
                isDisplayedTranslationCorrect = request.isDisplayedTranslationCorrect,
            )
        }

        // This training has no Tip action, so the precedence is just Skip, then a straight comparison.
        private fun resolveOutcome(request: SubmitTrueOrFalseAnswerRequest): TrueOrFalseStepOutcome =
            when {
                request.skipped -> TrueOrFalseStepOutcome.SKIPPED
                request.userAnsweredTrue == request.isDisplayedTranslationCorrect -> TrueOrFalseStepOutcome.CORRECT
                else -> TrueOrFalseStepOutcome.INCORRECT
            }

        private fun TrueOrFalseStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                TrueOrFalseStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                TrueOrFalseStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                TrueOrFalseStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
