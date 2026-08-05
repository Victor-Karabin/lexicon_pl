package com.lexicon.pl.domain.pronunciation

import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.domain.dictation.toWord
import com.lexicon.pl.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.pl.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.pl.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.pl.interactors.pronunciation.StartPronunciationSessionUseCase
import java.util.UUID
import javax.inject.Inject

class StartPronunciationSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : StartPronunciationSessionUseCase {
        override suspend fun invoke(request: StartPronunciationSessionRequest): PronunciationSessionResponse {
            val words = vocabularyRepository.getRandomItems(request.stepCount).map { it.toWord() }
            val steps =
                words.mapIndexed { index, word ->
                    PronunciationStepResponse(stepIndex = index, vocabularyItemId = word.id, expectedText = word.text)
                }
            return PronunciationSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }
    }
