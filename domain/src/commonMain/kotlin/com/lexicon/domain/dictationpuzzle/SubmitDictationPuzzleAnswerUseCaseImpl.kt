package com.lexicon.domain.dictationpuzzle

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.domain.training.toBoundary
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerResponse
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.interactors.training.StepOutcome

private const val TRAINING_TYPE_DICTATION_PUZZLE = "DICTATION_PUZZLE"

class SubmitDictationPuzzleAnswerUseCaseImpl(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val answerNormalizer: AnswerNormalizer,
    private val clock: Clock,
) : SubmitDictationPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitDictationPuzzleAnswerRequest): SubmitDictationPuzzleAnswerResponse {
        val outcome = resolveOutcome(request)

        trainingHistoryRepository.recordResult(
            TrainingResultBoundary(
                sessionId = request.sessionId,
                trainingType = TRAINING_TYPE_DICTATION_PUZZLE,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome.toBoundary(),
                tipUsed = request.tipUsed,
                completedAtEpochMillis = clock.nowEpochMillis(),
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
