package com.lexicon.domain.wordmatch

import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultRequest
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultResponse
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase
import com.lexicon.model.training.StepOutcome

private const val TRAINING_TYPE_WORD_MATCH = "WORD_MATCH"

class SubmitWordMatchStepResultUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
) : SubmitWordMatchStepResultUseCase {
    override suspend fun invoke(request: SubmitWordMatchStepResultRequest): SubmitWordMatchStepResultResponse {
        val outcome = resolveOutcome(request)

        request.vocabularyItemIds.forEach { vocabularyItemId ->
            recordAnswer(
                RecordedAnswer(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_WORD_MATCH,
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
