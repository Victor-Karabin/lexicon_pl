package com.lexicon.domain.imagetest

import com.lexicon.interactors.imagetest.SubmitImageTestAnswerRequest
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerResponse
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitImageTestAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
) : SubmitImageTestAnswerUseCase {
    override suspend fun invoke(request: SubmitImageTestAnswerRequest): SubmitImageTestAnswerResponse {
        val outcome = resolveOutcome(request)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TrainingType.IMAGE_TEST,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.correctOption,
                submittedAnswer = request.selectedOption.orEmpty(),
                outcome = outcome,
                tipUsed = false,
            ),
        )

        return SubmitImageTestAnswerResponse(outcome = outcome, correctOption = request.correctOption)
    }

    private fun resolveOutcome(request: SubmitImageTestAnswerRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            request.selectedOption == request.correctOption -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
