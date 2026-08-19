@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.dictationpuzzle

import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.domain.training.open
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleSessionResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionRequest
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.model.training.TrainingType
import kotlin.uuid.ExperimentalUuidApi

class StartDictationPuzzleSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartDictationPuzzleSessionUseCase {
    override suspend fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val words = vocabularyRepository.getRandomItems(stepCount, request.vocabularyIds).map { it }
        val steps =
            words.mapIndexed { index, word ->
                DictationPuzzleStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id.value,
                    expectedText = word.text,
                    translationText = word.translation,
                )
            }
        val sessionId = sessions.open(TrainingType.DICTATION_PUZZLE, words.map { it.id to it.text })
        return DictationPuzzleSessionResponse(sessionId = sessionId.value, steps = steps)
    }
}
