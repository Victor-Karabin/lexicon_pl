package com.lexicon.application.puzzle

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.training.submitTyped
import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerResponse
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.model.training.TrainingType

class SubmitPuzzleAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitPuzzleAnswerRequest): SubmitPuzzleAnswerResponse {
        val answer = sessions.submitTyped(
            training = TrainingType.PUZZLE,
            sessionId = request.sessionId,
            stepIndex = request.stepIndex,
            fallbackExpectedText = request.expectedText,
            fallbackVocabularyItemId = request.vocabularyItemId,
            submittedText = request.submittedText,
            skipped = request.skipped,
            tipUsed = request.tipUsed,
            recordAnswer = recordAnswer,
            matches = answerNormalizer::matches,
        )
        return SubmitPuzzleAnswerResponse(outcome = answer.outcome, expectedText = answer.expectedText)
    }
}
