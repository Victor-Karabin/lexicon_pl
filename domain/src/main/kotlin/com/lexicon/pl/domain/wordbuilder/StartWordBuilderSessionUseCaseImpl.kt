package com.lexicon.pl.domain.wordbuilder

import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.domain.dictation.toWord
import com.lexicon.pl.interactors.wordbuilder.StartWordBuilderSessionRequest
import com.lexicon.pl.interactors.wordbuilder.StartWordBuilderSessionUseCase
import com.lexicon.pl.interactors.wordbuilder.WordBuilderSessionResponse
import com.lexicon.pl.interactors.wordbuilder.WordBuilderStepResponse
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
