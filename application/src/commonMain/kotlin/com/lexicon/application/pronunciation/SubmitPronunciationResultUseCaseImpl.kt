package com.lexicon.application.pronunciation

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.training.recordOutcome
import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultResponse
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.StepOutcome

class SubmitPronunciationResultUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitPronunciationResultUseCase {
    override suspend fun invoke(request: SubmitPronunciationResultRequest): SubmitPronunciationResultResponse {
        val session = sessions.find(SessionId(request.sessionId))
        val step = session?.question(request.stepIndex)
        val expectedText = step?.expectedAnswer ?: request.expectedText
        val outcome = resolveOutcome(request, expectedText)

        if (session != null && sessions.recordOutcome(request.sessionId, request.stepIndex, outcome, request.tipUsed)) {
            recordAnswer(
                RecordedAnswer(
                    sessionId = request.sessionId,
                    trainingType = session.training,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = step?.wordId?.value ?: request.vocabularyItemId,
                    expectedAnswer = expectedText,
                    submittedAnswer = request.recognizedText,
                    outcome = outcome,
                    tipUsed = request.tipUsed,
                ),
            )
        }

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
