package com.lexicon.application.dictationpuzzle

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.training.recordOutcome
import com.lexicon.application.training.stepAt
import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerResponse
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitDictationPuzzleAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitDictationPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitDictationPuzzleAnswerRequest): SubmitDictationPuzzleAnswerResponse {
        val step = sessions.stepAt(request.sessionId, request.stepIndex)
        val expectedText = step?.expectedAnswer ?: request.expectedText
        val outcome = resolveOutcome(request, expectedText)
        sessions.recordOutcome(request.sessionId, request.stepIndex, outcome, request.tipUsed)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TrainingType.DICTATION_PUZZLE,
                stepIndex = request.stepIndex,
                vocabularyItemId = step?.wordId?.value ?: request.vocabularyItemId,
                expectedAnswer = expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitDictationPuzzleAnswerResponse(outcome = outcome, expectedText = expectedText)
    }

    private fun resolveOutcome(
        request: SubmitDictationPuzzleAnswerRequest,
        expectedText: String,
    ): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matches(expectedText, request.submittedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
