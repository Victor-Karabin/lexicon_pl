package com.lexicon.domain.pronunciation

import com.lexicon.boundary.SessionStore
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.domain.training.recordOutcome
import com.lexicon.domain.training.stepAt
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultResponse
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitPronunciationResultUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitPronunciationResultUseCase {
    override suspend fun invoke(request: SubmitPronunciationResultRequest): SubmitPronunciationResultResponse {
        val step = sessions.stepAt(request.sessionId, request.stepIndex)
        val expectedText = step?.expectedAnswer ?: request.expectedText
        val outcome = resolveOutcome(request, expectedText)
        sessions.recordOutcome(request.sessionId, request.stepIndex, outcome, request.tipUsed)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TrainingType.PRONUNCIATION_CHECK,
                stepIndex = request.stepIndex,
                vocabularyItemId = step?.wordId?.value ?: request.vocabularyItemId,
                expectedAnswer = expectedText,
                submittedAnswer = request.recognizedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitPronunciationResultResponse(outcome = outcome, expectedText = expectedText)
    }

    private fun resolveOutcome(
        request: SubmitPronunciationResultRequest,
        expectedText: String,
    ): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matchesSpoken(expectedText, request.recognizedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
