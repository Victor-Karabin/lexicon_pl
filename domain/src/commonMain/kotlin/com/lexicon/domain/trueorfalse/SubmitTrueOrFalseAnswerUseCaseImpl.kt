package com.lexicon.domain.trueorfalse

import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerResponse
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitTrueOrFalseAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
) : SubmitTrueOrFalseAnswerUseCase {
    override suspend fun invoke(request: SubmitTrueOrFalseAnswerRequest): SubmitTrueOrFalseAnswerResponse {
        val outcome = resolveOutcome(request)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TrainingType.TRUE_OR_FALSE,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.isDisplayedTranslationCorrect.toString(),
                submittedAnswer = request.userAnsweredTrue.toString(),
                outcome = outcome,
                tipUsed = false,
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
