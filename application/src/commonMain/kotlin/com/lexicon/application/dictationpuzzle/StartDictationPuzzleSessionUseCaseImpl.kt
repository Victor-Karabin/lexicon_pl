package com.lexicon.application.dictationpuzzle

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.open
import com.lexicon.application.training.randomWords
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleSessionResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionRequest
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.model.training.TrainingType

class StartDictationPuzzleSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartDictationPuzzleSessionUseCase {
    override suspend fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse {
        val words = vocabularyRepository.randomWords(stepCountResolver, request.stepCount, request.vocabularyIds)
        val steps =
            words.mapIndexed { index, word ->
                DictationPuzzleStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id.value,
                    expectedText = word.text,
                    translationText = word.translation,
                )
            }
        val sessionId = sessions.open(TrainingType.DICTATION_PUZZLE, words)
        return DictationPuzzleSessionResponse(sessionId = sessionId.value, steps = steps)
    }
}
