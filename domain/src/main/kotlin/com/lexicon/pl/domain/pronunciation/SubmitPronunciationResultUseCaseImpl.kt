package com.lexicon.pl.domain.pronunciation

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.boundary.TrainingResultBoundary
import com.lexicon.pl.boundary.TrainingResultOutcomeBoundary
import com.lexicon.pl.common.Clock
import com.lexicon.pl.domain.dictation.AnswerNormalizer
import com.lexicon.pl.interactors.pronunciation.PronunciationStepOutcome
import com.lexicon.pl.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.pl.interactors.pronunciation.SubmitPronunciationResultResponse
import com.lexicon.pl.interactors.pronunciation.SubmitPronunciationResultUseCase
import javax.inject.Inject

private const val TRAINING_TYPE_PRONUNCIATION_CHECK = "PRONUNCIATION_CHECK"

/** Default recognition threshold per "Pronunciation Check.rtf" (configurable by the application, default 70%). */
private const val RECOGNITION_CONFIDENCE_THRESHOLD = 0.7f

class SubmitPronunciationResultUseCaseImpl
    @Inject
    constructor(
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

        // Skip > Tip > confidence threshold when the recognizer reports one, else fall back to text matching.
        private fun resolveOutcome(request: SubmitPronunciationResultRequest): PronunciationStepOutcome {
            val confidence = request.confidence
            return when {
                request.skipped -> PronunciationStepOutcome.SKIPPED
                request.tipUsed -> PronunciationStepOutcome.INCORRECT
                confidence != null ->
                    if (confidence >= RECOGNITION_CONFIDENCE_THRESHOLD) {
                        PronunciationStepOutcome.CORRECT
                    } else {
                        PronunciationStepOutcome.INCORRECT
                    }
                answerNormalizer.matches(request.expectedText, request.recognizedText) -> PronunciationStepOutcome.CORRECT
                else -> PronunciationStepOutcome.INCORRECT
            }
        }

        private fun PronunciationStepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                PronunciationStepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                PronunciationStepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
                PronunciationStepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
            }
    }
