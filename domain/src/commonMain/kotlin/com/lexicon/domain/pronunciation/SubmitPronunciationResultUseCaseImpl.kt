package com.lexicon.domain.pronunciation

import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultResponse
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome

private const val TRAINING_TYPE_PRONUNCIATION_CHECK = "PRONUNCIATION_CHECK"

class SubmitPronunciationResultUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
) : SubmitPronunciationResultUseCase {
    override suspend fun invoke(request: SubmitPronunciationResultRequest): SubmitPronunciationResultResponse {
        val outcome = resolveOutcome(request)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TRAINING_TYPE_PRONUNCIATION_CHECK,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.expectedText,
                submittedAnswer = request.recognizedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitPronunciationResultResponse(outcome = outcome, expectedText = request.expectedText)
    }

    private fun resolveOutcome(request: SubmitPronunciationResultRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matchesSpoken(request.expectedText, request.recognizedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
