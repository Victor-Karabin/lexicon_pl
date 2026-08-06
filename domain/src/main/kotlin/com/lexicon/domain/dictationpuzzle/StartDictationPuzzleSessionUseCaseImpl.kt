package com.lexicon.domain.dictationpuzzle

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.toWord
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleSessionResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionRequest
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import java.util.UUID
import javax.inject.Inject

class StartDictationPuzzleSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : StartDictationPuzzleSessionUseCase {
        override suspend fun invoke(request: StartDictationPuzzleSessionRequest): DictationPuzzleSessionResponse {
            val words = vocabularyRepository.getRandomItems(request.stepCount).map { it.toWord() }
            val steps =
                words.mapIndexed { index, word ->
                    DictationPuzzleStepResponse(
                        stepIndex = index,
                        vocabularyItemId = word.id,
                        expectedText = word.text,
                        translationText = word.translation,
                    )
                }
            return DictationPuzzleSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }
    }
