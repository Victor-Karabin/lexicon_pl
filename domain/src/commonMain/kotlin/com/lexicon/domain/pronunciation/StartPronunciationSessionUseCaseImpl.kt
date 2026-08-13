@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.pronunciation

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.toWord
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StartPronunciationSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
) : StartPronunciationSessionUseCase {
    override suspend fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds).map { it.toWord() }
        val steps =
            words.mapIndexed { index, word ->
                PronunciationStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id,
                    expectedText = word.text,
                    clueText = word.translation,
                    transcription = word.transcription,
                )
            }
        return PronunciationSessionResponse(sessionId = Uuid.random().toString(), steps = steps)
    }
}
