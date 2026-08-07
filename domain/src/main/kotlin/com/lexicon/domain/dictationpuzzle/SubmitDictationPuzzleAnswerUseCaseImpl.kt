package com.lexicon.domain.dictationpuzzle

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepOutcome
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerResponse
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import javax.inject.Inject

private const val TRAINING_TYPE_DICTATION_PUZZLE = "DICTATION_PUZZLE"

class SubmitDictationPuzzleAnswerUseCaseImpl
    @Inject
    constructor(
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

        // The step outcome reflects the input alone; tip usage is recorded separately
        // (tipUsed above) and surfaced on the Results screen rather than forcing Incorrect here.
        private fun resolveOutcome(request: SubmitDictationPuzzleAnswerRequest): DictationPuzzleStepOutcome =
            when {
                request.skipped -> DictationPuzzleStepOutcome.SKIPPED
                answerNormalizer.matches(request.expectedText, request.submittedText) -> DictationPuzzleStepOutcome.CORRECT
                else -> DictationPuzzleStepOutcome.INCORRECT
            }

        private fun DictationPuzzleStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                DictationPuzzleStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                DictationPuzzleStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                DictationPuzzleStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
