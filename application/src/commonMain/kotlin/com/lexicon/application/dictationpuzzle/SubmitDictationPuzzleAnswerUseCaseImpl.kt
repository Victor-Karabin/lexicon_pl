package com.lexicon.application.dictationpuzzle

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.training.submitTyped
import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerResponse
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.model.training.TrainingType

class SubmitDictationPuzzleAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitDictationPuzzleAnswerUseCase {
    override suspend fun invoke(request: SubmitDictationPuzzleAnswerRequest): SubmitDictationPuzzleAnswerResponse {
        val answer = sessions.submitTyped(
            training = TrainingType.DICTATION_PUZZLE,
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
        return SubmitDictationPuzzleAnswerResponse(outcome = answer.outcome, expectedText = answer.expectedText)
    }
}
