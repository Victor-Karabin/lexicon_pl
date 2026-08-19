package com.lexicon.domain.dictation

import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerResponse
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitDictationAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitDictationAnswerUseCase {
    override suspend fun invoke(request: SubmitDictationAnswerRequest): SubmitDictationAnswerResponse {
        val id = SessionId(request.sessionId)
        val session = sessions.find(id)
        val step = session?.step(request.stepIndex)

        val expectedText = step?.expectedAnswer ?: request.expectedText
        val outcome = resolveOutcome(request, expectedText)

        if (session != null && step != null) {
            sessions.save(session.answer(step.index, outcome, request.tipUsed))
        }

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TrainingType.DICTATION,
                stepIndex = request.stepIndex,
                vocabularyItemId = step?.wordId?.value ?: request.vocabularyItemId,
                expectedAnswer = expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitDictationAnswerResponse(outcome = outcome, expectedText = expectedText)
    }

    private fun resolveOutcome(
        request: SubmitDictationAnswerRequest,
        expectedText: String,
    ): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matches(expectedText, request.submittedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
