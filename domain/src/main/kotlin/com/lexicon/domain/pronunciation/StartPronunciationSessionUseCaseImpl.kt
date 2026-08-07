package com.lexicon.domain.pronunciation

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.toWord
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
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
                    PronunciationStepResponse(
                        stepIndex = index,
                        vocabularyItemId = word.id,
                        expectedText = word.text,
                        clueText = word.translation,
                        transcription = word.transcription,
                    )
                }
            return PronunciationSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }
    }
