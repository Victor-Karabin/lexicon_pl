package com.lexicon.application.dictation

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.open
import com.lexicon.application.training.randomWords
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.dictation.DictationSessionResponse
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.model.training.TrainingType

class StartDictationSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartDictationSessionUseCase {
    override suspend fun invoke(request: StartDictationSessionRequest): DictationSessionResponse {
        val words = vocabularyRepository.randomWords(stepCountResolver, request.stepCount, request.vocabularyIds)

        val sessionId = sessions.open(TrainingType.DICTATION, words)

        return DictationSessionResponse(
            sessionId = sessionId.value,
            steps = words.mapIndexed { index, word ->
                DictationStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id.value,
                    expectedText = word.text,
                    translationText = word.translation,
                )
            },
        )
    }
}
