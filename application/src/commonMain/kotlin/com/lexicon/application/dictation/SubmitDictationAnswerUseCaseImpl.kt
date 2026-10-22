package com.lexicon.application.dictation

import com.lexicon.application.training.submitTyped
import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerResponse
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.model.training.TrainingType

class SubmitDictationAnswerUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitDictationAnswerUseCase {
    override suspend fun invoke(request: SubmitDictationAnswerRequest): SubmitDictationAnswerResponse {
        val answer = sessions.submitTyped(
            training = TrainingType.DICTATION,
            sessionId = request.sessionId,
            stepIndex = request.stepIndex,
            fallbackExpectedText = request.expectedText,
            fallbackVocabularyItemId = request.vocabularyItemId,
            submittedText = request.submittedText,
            skipped = request.skipped,
            tipUsed = request.tipUsed,
            recordAnswer = recordAnswer,
            matches = answerNormalizer::matchesIgnoringPunctuation,
        )
        return SubmitDictationAnswerResponse(outcome = answer.outcome, expectedText = answer.expectedText)
    }
}
