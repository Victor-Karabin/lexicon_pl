package com.lexicon.domain.puzzle

import com.lexicon.boundary.SessionStore
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.domain.training.recordOutcome
import com.lexicon.domain.training.stepAt
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerResponse
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitPuzzleAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitPuzzleAnswerRequest): SubmitPuzzleAnswerResponse {
        val step = sessions.stepAt(request.sessionId, request.stepIndex)
        val expectedText = step?.expectedAnswer ?: request.expectedText
        val outcome = resolveOutcome(request, expectedText)
        sessions.recordOutcome(request.sessionId, request.stepIndex, outcome, request.tipUsed)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TrainingType.PUZZLE,
                stepIndex = request.stepIndex,
                vocabularyItemId = step?.wordId?.value ?: request.vocabularyItemId,
                expectedAnswer = expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitPuzzleAnswerResponse(outcome = outcome, expectedText = expectedText)
    }

    private fun resolveOutcome(
        request: SubmitPuzzleAnswerRequest,
        expectedText: String,
    ): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matches(expectedText, request.submittedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
