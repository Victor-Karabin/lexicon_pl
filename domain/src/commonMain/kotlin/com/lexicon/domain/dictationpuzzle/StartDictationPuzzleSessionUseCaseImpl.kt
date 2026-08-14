@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.dictationpuzzle

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.toWord
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleSessionResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionRequest
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StartDictationPuzzleSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
) : StartDictationPuzzleSessionUseCase {
    override suspend fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds).map { it.toWord() }
        val steps =
            words.mapIndexed { index, word ->
                DictationPuzzleStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id,
                    expectedText = word.text,
                    translationText = word.translation,
                )
            }
        return DictationPuzzleSessionResponse(sessionId = Uuid.random().toString(), steps = steps)
    }
}
