@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.dictation

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.dictation.DictationSessionResponse
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StartDictationSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
) : StartDictationSessionUseCase {
    override suspend fun invoke(request: StartDictationSessionRequest): DictationSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds).map { it }
        val steps =
            words.mapIndexed { index, word ->
                DictationStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id.value,
                    expectedText = word.text,
                    translationText = word.translation,
                )
            }
        return DictationSessionResponse(sessionId = Uuid.random().toString(), steps = steps)
    }
}
