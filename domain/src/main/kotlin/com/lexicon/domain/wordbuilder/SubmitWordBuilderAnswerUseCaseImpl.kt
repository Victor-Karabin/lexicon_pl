package com.lexicon.domain.wordbuilder

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.wordbuilder.SubmitWordBuilderAnswerRequest
import com.lexicon.interactors.wordbuilder.SubmitWordBuilderAnswerResponse
import com.lexicon.interactors.wordbuilder.SubmitWordBuilderAnswerUseCase
import com.lexicon.interactors.wordbuilder.WordBuilderStepOutcome
import javax.inject.Inject

private const val TRAINING_TYPE_WORD_BUILDER = "WORD_BUILDER"

class SubmitWordBuilderAnswerUseCaseImpl
    @Inject
    constructor(
        private val trainingHistoryRepository: TrainingHistoryRepository,
        private val answerNormalizer: AnswerNormalizer,
        private val clock: Clock,
    ) : SubmitWordBuilderAnswerUseCase {
        override suspend fun invoke(request: SubmitWordBuilderAnswerRequest): SubmitWordBuilderAnswerResponse {
            val outcome = resolveOutcome(request)

            trainingHistoryRepository.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_WORD_BUILDER,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = request.vocabularyItemId,
                    expectedAnswer = request.expectedText,
                    submittedAnswer = request.submittedText,
                    outcome = outcome.toBoundary(),
                    tipUsed = request.tipUsed,
                    completedAtEpochMillis = clock.nowEpochMillis(),
                ),
            )

            return SubmitWordBuilderAnswerResponse(outcome = outcome, expectedText = request.expectedText)
        }

        // The step outcome reflects the input alone; tip usage is recorded separately
        // (tipUsed above) and surfaced on the Results screen rather than forcing Incorrect here.
        private fun resolveOutcome(request: SubmitWordBuilderAnswerRequest): WordBuilderStepOutcome =
            when {
                request.skipped -> WordBuilderStepOutcome.SKIPPED
                answerNormalizer.matches(request.expectedText, request.submittedText) -> WordBuilderStepOutcome.CORRECT
                else -> WordBuilderStepOutcome.INCORRECT
            }

        private fun WordBuilderStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                WordBuilderStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                WordBuilderStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                WordBuilderStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
