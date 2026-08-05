package com.lexicon.domain.dictation

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.dictation.DictationSessionResponse
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import java.util.UUID
import javax.inject.Inject

class StartDictationSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : StartDictationSessionUseCase {
        override suspend fun invoke(request: StartDictationSessionRequest): DictationSessionResponse {
            val words = vocabularyRepository.getRandomItems(request.stepCount).map { it.toWord() }
            val steps =
                words.mapIndexed { index, word ->
                    DictationStepResponse(
                        stepIndex = index,
                        vocabularyItemId = word.id,
                        expectedText = word.text,
                    )
                }
            return DictationSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }
    }
