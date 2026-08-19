package com.lexicon.domain.dictation

import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerResponse
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome

private const val TRAINING_TYPE_DICTATION = "DICTATION"

class SubmitDictationAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
) : SubmitDictationAnswerUseCase {
    override suspend fun invoke(request: SubmitDictationAnswerRequest): SubmitDictationAnswerResponse {
        val outcome = resolveOutcome(request)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TRAINING_TYPE_DICTATION,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitDictationAnswerResponse(outcome = outcome, expectedText = request.expectedText)
    }

    private fun resolveOutcome(request: SubmitDictationAnswerRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matches(request.expectedText, request.submittedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
