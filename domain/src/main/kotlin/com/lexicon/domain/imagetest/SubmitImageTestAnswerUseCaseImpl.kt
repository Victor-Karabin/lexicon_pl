package com.lexicon.domain.imagetest

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.imagetest.ImageTestStepOutcome
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerRequest
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerResponse
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import javax.inject.Inject

private const val TRAINING_TYPE_IMAGE_TEST = "IMAGE_TEST"

class SubmitImageTestAnswerUseCaseImpl
    @Inject
    constructor(
        private val trainingHistoryRepository: TrainingHistoryRepository,
        private val clock: Clock,
    ) : SubmitImageTestAnswerUseCase {
        override suspend fun invoke(request: SubmitImageTestAnswerRequest): SubmitImageTestAnswerResponse {
            val outcome = resolveOutcome(request)

            trainingHistoryRepository.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_IMAGE_TEST,
                    stepIndex = request.stepIndex,
                    vocabularyItemId = request.vocabularyItemId,
                    expectedAnswer = request.correctOption,
                    submittedAnswer = request.selectedOption.orEmpty(),
                    outcome = outcome.toBoundary(),
                    tipUsed = false,
                    completedAtEpochMillis = clock.nowEpochMillis(),
                ),
            )

            return SubmitImageTestAnswerResponse(outcome = outcome, correctOption = request.correctOption)
        }

        private fun resolveOutcome(request: SubmitImageTestAnswerRequest): ImageTestStepOutcome =
            when {
                request.skipped -> ImageTestStepOutcome.SKIPPED
                request.selectedOption == request.correctOption -> ImageTestStepOutcome.CORRECT
                else -> ImageTestStepOutcome.INCORRECT
            }

        private fun ImageTestStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                ImageTestStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                ImageTestStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                ImageTestStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
