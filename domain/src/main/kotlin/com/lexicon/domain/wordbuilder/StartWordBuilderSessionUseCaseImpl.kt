package com.lexicon.domain.wordbuilder

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.toWord
import com.lexicon.interactors.wordbuilder.StartWordBuilderSessionRequest
import com.lexicon.interactors.wordbuilder.StartWordBuilderSessionUseCase
import com.lexicon.interactors.wordbuilder.WordBuilderSessionResponse
import com.lexicon.interactors.wordbuilder.WordBuilderStepResponse
import java.util.UUID
import javax.inject.Inject

class StartWordBuilderSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : StartWordBuilderSessionUseCase {
        override suspend fun invoke(request: StartWordBuilderSessionRequest): WordBuilderSessionResponse {
            val words = vocabularyRepository.getRandomItems(request.stepCount).map { it.toWord() }
            val steps =
                words.mapIndexed { index, word ->
                    WordBuilderStepResponse(
                        stepIndex = index,
                        vocabularyItemId = word.id,
                        expectedText = word.text,
                        clueText = word.translation,
                    )
                }
            return WordBuilderSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }
    }
