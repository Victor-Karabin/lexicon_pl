@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.pronunciation

import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.domain.training.open
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.model.training.TrainingType
import kotlin.uuid.ExperimentalUuidApi

class StartPronunciationSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartPronunciationSessionUseCase {
    override suspend fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds).map { it }
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
        val sessionId = sessions.open(TrainingType.PRONUNCIATION_CHECK, words.map { it.id to it.text })
        return PronunciationSessionResponse(sessionId = sessionId.value, steps = steps)
    }
}
