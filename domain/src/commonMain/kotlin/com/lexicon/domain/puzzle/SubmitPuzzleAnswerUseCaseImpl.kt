package com.lexicon.domain.puzzle

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.domain.training.toBoundary
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerResponse
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.training.StepOutcome

private const val TRAINING_TYPE_PUZZLE = "PUZZLE"

class SubmitPuzzleAnswerUseCaseImpl(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val answerNormalizer: AnswerNormalizer,
    private val clock: Clock,
) : SubmitPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitPuzzleAnswerRequest): SubmitPuzzleAnswerResponse {
        val outcome = resolveOutcome(request)

        trainingHistoryRepository.recordResult(
            TrainingResultBoundary(
                sessionId = request.sessionId,
                trainingType = TRAINING_TYPE_PUZZLE,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.expectedText,
                submittedAnswer = request.submittedText,
                outcome = outcome.toBoundary(),
                tipUsed = request.tipUsed,
                completedAtEpochMillis = clock.nowEpochMillis(),
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
