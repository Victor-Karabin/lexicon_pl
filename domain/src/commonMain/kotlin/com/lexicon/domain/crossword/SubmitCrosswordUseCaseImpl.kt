package com.lexicon.domain.crossword

import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.crossword.CrosswordWordResult
import com.lexicon.interactors.crossword.SubmitCrosswordRequest
import com.lexicon.interactors.crossword.SubmitCrosswordResponse
import com.lexicon.interactors.crossword.SubmitCrosswordUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome

private const val TRAINING_TYPE_CROSSWORD = "CROSSWORD"

class SubmitCrosswordUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
) : SubmitCrosswordUseCase {
    override suspend fun invoke(request: SubmitCrosswordRequest): SubmitCrosswordResponse {
        val wordResults = request.words.mapIndexed { index, submission ->
            val outcome = if (answerNormalizer.matches(submission.expectedText, submission.submittedText)) {
                StepOutcome.CORRECT
            } else {
                StepOutcome.INCORRECT
            }
            recordAnswer(
                RecordedAnswer(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_TYPE_CROSSWORD,
                    stepIndex = index,
                    vocabularyItemId = submission.vocabularyItemId,
                    expectedAnswer = submission.expectedText,
                    submittedAnswer = submission.submittedText,
                    outcome = outcome,
                    tipUsed = submission.tipUsed,
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
