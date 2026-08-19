package com.lexicon.domain.pronunciation

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.domain.training.toBoundary
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultResponse
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.training.StepOutcome

private const val TRAINING_TYPE_PRONUNCIATION_CHECK = "PRONUNCIATION_CHECK"

class SubmitPronunciationResultUseCaseImpl(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val answerNormalizer: AnswerNormalizer,
    private val clock: Clock,
) : SubmitPronunciationResultUseCase {
    override suspend fun invoke(request: SubmitPronunciationResultRequest): SubmitPronunciationResultResponse {
        val outcome = resolveOutcome(request)

        trainingHistoryRepository.recordResult(
            TrainingResultBoundary(
                sessionId = request.sessionId,
                trainingType = TRAINING_TYPE_PRONUNCIATION_CHECK,
                stepIndex = request.stepIndex,
                vocabularyItemId = request.vocabularyItemId,
                expectedAnswer = request.expectedText,
                submittedAnswer = request.recognizedText,
                outcome = outcome.toBoundary(),
                tipUsed = request.tipUsed,
                completedAtEpochMillis = clock.nowEpochMillis(),
            ),
        )

        return SubmitPronunciationResultResponse(outcome = outcome, expectedText = request.expectedText)
    }

    private fun resolveOutcome(request: SubmitPronunciationResultRequest): StepOutcome =
        when {
            request.skipped -> StepOutcome.SKIPPED
            answerNormalizer.matchesSpoken(request.expectedText, request.recognizedText) -> StepOutcome.CORRECT
            else -> StepOutcome.INCORRECT
        }
}
