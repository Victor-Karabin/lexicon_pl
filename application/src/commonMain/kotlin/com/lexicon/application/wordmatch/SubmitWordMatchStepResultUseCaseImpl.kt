package com.lexicon.application.wordmatch

import com.lexicon.application.training.recordOutcome
import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultRequest
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultResponse
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitWordMatchStepResultUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val sessions: SessionStore,
) : SubmitWordMatchStepResultUseCase {
    override suspend fun invoke(request: SubmitWordMatchStepResultRequest): SubmitWordMatchStepResultResponse {
        val outcome = resolveOutcome(request)
        sessions.recordOutcome(request.sessionId, request.stepIndex, outcome)

        request.vocabularyItemIds.forEach { vocabularyItemId ->
            recordAnswer(
                RecordedAnswer(
                    sessionId = request.sessionId,
                    trainingType = TrainingType.WORD_MATCH,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = vocabularyItemId,
                    expectedAnswer = "matched",
                    submittedAnswer = request.incorrectAttempts.toString(),
                    outcome = outcome,
                    tipUsed = false,
                ),
            )
        }

        return SubmitWordMatchStepResultResponse(outcome = outcome)
    }

    private fun resolveOutcome(request: SubmitWordMatchStepResultRequest): StepOutcome =
        if (request.incorrectAttempts == 0) StepOutcome.CORRECT else StepOutcome.INCORRECT
}
