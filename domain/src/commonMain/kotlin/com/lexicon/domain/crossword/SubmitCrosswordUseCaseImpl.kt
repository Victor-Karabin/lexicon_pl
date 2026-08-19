package com.lexicon.domain.crossword

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.domain.training.toBoundary
import com.lexicon.interactors.crossword.CrosswordWordResult
import com.lexicon.interactors.crossword.SubmitCrosswordRequest
import com.lexicon.interactors.crossword.SubmitCrosswordResponse
import com.lexicon.interactors.crossword.SubmitCrosswordUseCase
import com.lexicon.interactors.training.StepOutcome

private const val TRAINING_TYPE_CROSSWORD = "CROSSWORD"

class SubmitCrosswordUseCaseImpl(
    private val trainingHistoryRepository: TrainingHistoryRepository,
    private val answerNormalizer: AnswerNormalizer,
    private val clock: Clock,
) : SubmitCrosswordUseCase {
    override suspend fun invoke(request: SubmitCrosswordRequest): SubmitCrosswordResponse {
        val completedAt = clock.nowEpochMillis()

        val wordResults = request.words.mapIndexed { index, submission ->
            val outcome = if (answerNormalizer.matches(submission.expectedText, submission.submittedText)) {
                StepOutcome.CORRECT
            } else {
                StepOutcome.INCORRECT
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

        val isFullyCorrect = wordResults.isNotEmpty() &&
            wordResults.all { it.outcome == StepOutcome.CORRECT } &&
            wordResults.none { it.tipUsed }

        return SubmitCrosswordResponse(wordResults = wordResults, isFullyCorrect = isFullyCorrect)
    }
}
