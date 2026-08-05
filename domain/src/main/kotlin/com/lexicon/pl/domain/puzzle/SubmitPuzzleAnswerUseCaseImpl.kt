package com.lexicon.pl.domain.puzzle

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.boundary.TrainingResultBoundary
import com.lexicon.pl.boundary.TrainingResultOutcomeBoundary
import com.lexicon.pl.common.Clock
import com.lexicon.pl.domain.dictation.AnswerNormalizer
import com.lexicon.pl.interactors.puzzle.PuzzleStepOutcome
import com.lexicon.pl.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.pl.interactors.puzzle.SubmitPuzzleAnswerResponse
import com.lexicon.pl.interactors.puzzle.SubmitPuzzleAnswerUseCase
import javax.inject.Inject

private const val TRAINING_TYPE_PUZZLE = "PUZZLE"

class SubmitPuzzleAnswerUseCaseImpl
    @Inject
    constructor(
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

        private fun resolveOutcome(request: SubmitPuzzleAnswerRequest): PuzzleStepOutcome =
            when {
                request.skipped -> PuzzleStepOutcome.SKIPPED
                request.tipUsed -> PuzzleStepOutcome.INCORRECT
                answerNormalizer.matches(request.expectedText, request.submittedText) -> PuzzleStepOutcome.CORRECT
                else -> PuzzleStepOutcome.INCORRECT
            }

        private fun PuzzleStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                PuzzleStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                PuzzleStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                PuzzleStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
