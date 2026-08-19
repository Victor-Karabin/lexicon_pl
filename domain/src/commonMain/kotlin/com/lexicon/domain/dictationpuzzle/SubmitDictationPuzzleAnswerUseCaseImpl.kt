package com.lexicon.domain.dictationpuzzle

import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerResponse
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome

private const val TRAINING_TYPE_DICTATION_PUZZLE = "DICTATION_PUZZLE"

class SubmitDictationPuzzleAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
) : SubmitDictationPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitDictationPuzzleAnswerRequest): SubmitDictationPuzzleAnswerResponse {
        val outcome = resolveOutcome(request)

        recordAnswer(
            RecordedAnswer(
                sessionId = request.sessionId,
                trainingType = TRAINING_TYPE_DICTATION_PUZZLE,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome,
                tipUsed = request.tipUsed,
            ),
        )

        return SubmitDictationPuzzleAnswerResponse(outcome = outcome, expectedText = request.expectedText)
    }

    private fun resolveOutcome(request: SubmitDictationPuzzleAnswerRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matches(request.expectedText, request.submittedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
