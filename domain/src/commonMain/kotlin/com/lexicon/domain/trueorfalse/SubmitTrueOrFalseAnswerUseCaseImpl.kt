package com.lexicon.domain.trueorfalse

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerResponse
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepOutcome

private const val TRAINING_TYPE_TRUE_OR_FALSE = "TRUE_OR_FALSE"

class SubmitTrueOrFalseAnswerUseCaseImpl(
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
                submittedAnswer = request.userAnsweredTrue.toString(),
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

    private fun resolveOutcome(request: SubmitTrueOrFalseAnswerRequest): TrueOrFalseStepOutcome =
        if (request.userAnsweredTrue == request.isDisplayedTranslationCorrect) {
            TrueOrFalseStepOutcome.CORRECT
        } else {
            TrueOrFalseStepOutcome.INCORRECT
        }

    private fun TrueOrFalseStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
        when (this) {
            TrueOrFalseStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
            TrueOrFalseStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
        }
}
