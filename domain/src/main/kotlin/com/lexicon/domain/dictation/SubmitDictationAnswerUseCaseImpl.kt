package com.lexicon.domain.dictation

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.dictation.DictationStepOutcome
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerResponse
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import javax.inject.Inject

private const val TRAINING_TYPE_DICTATION = "DICTATION"

class SubmitDictationAnswerUseCaseImpl
    @Inject
    constructor(
        private val trainingHistoryRepository: TrainingHistoryRepository,
        private val answerNormalizer: AnswerNormalizer,
        private val clock: Clock,
    ) : SubmitDictationAnswerUseCase {
        override suspend fun invoke(request: SubmitDictationAnswerRequest): SubmitDictationAnswerResponse {
            val outcome = resolveOutcome(request)

            trainingHistoryRepository.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_DICTATION,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = request.vocabularyItemId,
                    expectedAnswer = request.expectedText,
                    submittedAnswer = request.submittedText,
                    outcome = outcome.toBoundary(),
                    tipUsed = request.tipUsed,
                    completedAtEpochMillis = clock.nowEpochMillis(),
                ),
            )

            return SubmitDictationAnswerResponse(outcome = outcome, expectedText = request.expectedText)
        }

        // Skip always wins, then a used Tip always forces Incorrect even on a matching answer,
        // per "Trainings - common.rtf" §12/§14.
        private fun resolveOutcome(request: SubmitDictationAnswerRequest): DictationStepOutcome =
            when {
                request.skipped -> DictationStepOutcome.SKIPPED
                request.tipUsed -> DictationStepOutcome.INCORRECT
                answerNormalizer.matches(request.expectedText, request.submittedText) -> DictationStepOutcome.CORRECT
                else -> DictationStepOutcome.INCORRECT
            }

        private fun DictationStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                DictationStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                DictationStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                DictationStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
