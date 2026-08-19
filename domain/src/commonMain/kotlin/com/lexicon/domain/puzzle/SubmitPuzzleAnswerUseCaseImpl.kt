package com.lexicon.domain.puzzle

import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerResponse
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome

private const val TRAINING_TYPE_PUZZLE = "PUZZLE"

class SubmitPuzzleAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
) : SubmitPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitPuzzleAnswerRequest): SubmitPuzzleAnswerResponse {
        val outcome = resolveOutcome(request)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TRAINING_TYPE_PUZZLE,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitPuzzleAnswerResponse(outcome = outcome, expectedText = request.expectedText)
    }

    private fun resolveOutcome(request: SubmitPuzzleAnswerRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matches(request.expectedText, request.submittedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
