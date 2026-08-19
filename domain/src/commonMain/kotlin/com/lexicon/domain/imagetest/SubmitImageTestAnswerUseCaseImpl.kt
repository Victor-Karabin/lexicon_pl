package com.lexicon.domain.imagetest

import com.lexicon.boundary.SessionStore
import com.lexicon.domain.training.recordOutcome
import com.lexicon.domain.training.stepAt
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerRequest
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerResponse
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitImageTestAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val sessions: SessionStore,
) : SubmitImageTestAnswerUseCase {
    override suspend fun invoke(request: SubmitImageTestAnswerRequest): SubmitImageTestAnswerResponse {
        val step = sessions.stepAt(request.sessionId, request.stepIndex)
        val correctOption = step?.expectedAnswer ?: request.correctOption
        val outcome = resolveOutcome(request, correctOption)
        sessions.recordOutcome(request.sessionId, request.stepIndex, outcome)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TrainingType.IMAGE_TEST,
                stepIndex = request.stepIndex,
                vocabularyItemId = step?.wordId?.value ?: request.vocabularyItemId,
                expectedAnswer = correctOption,
                submittedAnswer = request.selectedOption.orEmpty(),
                outcome = outcome,
                tipUsed = false,
            ),
        )

        return SubmitImageTestAnswerResponse(outcome = outcome, correctOption = correctOption)
    }

    private fun resolveOutcome(
        request: SubmitImageTestAnswerRequest,
        correctOption: String,
    ): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            request.selectedOption == correctOption -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
