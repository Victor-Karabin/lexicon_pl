package com.lexicon.domain.trueorfalse

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.training.toBoundary
import com.lexicon.interactors.training.StepOutcome
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerResponse
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase

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

    private fun resolveOutcome(request: SubmitTrueOrFalseAnswerRequest): StepOutcome =
        if (request.userAnsweredTrue == request.isDisplayedTranslationCorrect) {
            StepOutcome.CORRECT
        } else {
            StepOutcome.INCORRECT
        }
}
