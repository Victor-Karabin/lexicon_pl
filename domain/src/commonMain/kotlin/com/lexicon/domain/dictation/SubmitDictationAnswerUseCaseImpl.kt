package com.lexicon.domain.dictation

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.dictation.DictationStepOutcome
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerResponse
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase

private const val TRAINING_TYPE_DICTATION = "DICTATION"

class SubmitDictationAnswerUseCaseImpl(
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

    private fun resolveOutcome(request: SubmitDictationAnswerRequest): DictationStepOutcome =
        when {
            request.skipped -> DictationStepOutcome.SKIPPED
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
