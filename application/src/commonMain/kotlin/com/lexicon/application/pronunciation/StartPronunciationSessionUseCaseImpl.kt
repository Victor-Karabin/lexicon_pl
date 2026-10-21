package com.lexicon.application.pronunciation

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.open
import com.lexicon.application.training.randomWords
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.model.training.TrainingType

class StartPronunciationSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartPronunciationSessionUseCase {
    override suspend fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse {
        val words = vocabularyRepository.randomWords(stepCountResolver, request.stepCount, request.vocabularyIds)
        val steps =
            words.mapIndexed { index, word ->
                PronunciationStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id.value,
                    expectedText = word.text,
                    clueText = word.translation,
                    transcription = word.transcription,
                )
            }
        val sessionId = sessions.open(TrainingType.PRONUNCIATION_CHECK, words)
        return PronunciationSessionResponse(sessionId = sessionId.value, steps = steps)
    }
}
