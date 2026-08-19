package com.lexicon.domain.memorycards

import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultRequest
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultResponse
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitMemoryCardsStepResultUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
) : SubmitMemoryCardsStepResultUseCase {
    override suspend fun invoke(request: SubmitMemoryCardsStepResultRequest): SubmitMemoryCardsStepResultResponse {
        val outcome = resolveOutcome(request)

        request.vocabularyItemIds.forEach { vocabularyItemId ->
            recordAnswer(
                RecordedAnswer(
                    sessionId = request.sessionId,
                    trainingType = TrainingType.MEMORY_CARDS,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = vocabularyItemId,
                    expectedAnswer = "matched",
                    submittedAnswer = request.incorrectAttempts.toString(),
                    outcome = outcome,
                    tipUsed = false,
                ),
            )
        }

        return SubmitMemoryCardsStepResultResponse(outcome = outcome)
    }

    private fun resolveOutcome(request: SubmitMemoryCardsStepResultRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            request.incorrectAttempts == 0 -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
