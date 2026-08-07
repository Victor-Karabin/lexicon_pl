package com.lexicon.domain.crossword

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.crossword.CrosswordWordOutcome
import com.lexicon.interactors.crossword.CrosswordWordResult
import com.lexicon.interactors.crossword.SubmitCrosswordRequest
import com.lexicon.interactors.crossword.SubmitCrosswordResponse
import com.lexicon.interactors.crossword.SubmitCrosswordUseCase
import javax.inject.Inject

private const val TRAINING_TYPE_CROSSWORD = "CROSSWORD"

class SubmitCrosswordUseCaseImpl
    @Inject
    constructor(
        private val trainingHistoryRepository: TrainingHistoryRepository,
        private val answerNormalizer: AnswerNormalizer,
        private val clock: Clock,
    ) : SubmitCrosswordUseCase {
        override suspend fun invoke(request: SubmitCrosswordRequest): SubmitCrosswordResponse {
            val completedAt = clock.nowEpochMillis()

            val wordResults = request.words.mapIndexed { index, submission ->
                val outcome = if (answerNormalizer.matches(submission.expectedText, submission.submittedText)) {
                    CrosswordWordOutcome.CORRECT
                } else {
                    CrosswordWordOutcome.INCORRECT
                }
                trainingHistoryRepository.recordResult(
                    TrainingResultBoundary(
                        sessionId = request.sessionId,
                        trainingType = TRAINING_TYPE_CROSSWORD,
                        stepIndex = index,
                        vocabularyItemId = submission.vocabularyItemId,
                        expectedAnswer = submission.expectedText,
                        submittedAnswer = submission.submittedText,
                        outcome = outcome.toBoundary(),
                        tipUsed = submission.tipUsed,
                        completedAtEpochMillis = completedAt,
                    ),
                )
                CrosswordWordResult(submission.vocabularyItemId, submission.expectedText, outcome, submission.tipUsed)
            }

            // Per spec: any Tip usage anywhere in the puzzle makes the whole crossword Incorrect,
            // even if every individual word is spelled correctly.
            val isFullyCorrect = wordResults.all { it.outcome == CrosswordWordOutcome.CORRECT } &&
                wordResults.none { it.tipUsed }

            return SubmitCrosswordResponse(wordResults = wordResults, isFullyCorrect = isFullyCorrect)
        }

        private fun CrosswordWordOutcome.toBoundary(): TrainingResultOutcomeBoundary =
            when (this) {
                CrosswordWordOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
                CrosswordWordOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
            }
    }
