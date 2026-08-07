package com.lexicon.domain.wordmatch

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.wordmatch.StartWordMatchSessionRequest
import com.lexicon.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.interactors.wordmatch.WordMatchPairResponse
import com.lexicon.interactors.wordmatch.WordMatchSessionResponse
import com.lexicon.interactors.wordmatch.WordMatchStepResponse
import java.util.UUID
import javax.inject.Inject

class StartWordMatchSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : StartWordMatchSessionUseCase {
        override suspend fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse {
            val pairs =
                vocabularyRepository.getRandomItems(request.stepCount).map { item ->
                    WordMatchPairResponse(vocabularyItemId = item.id, word = item.text, translation = item.translation)
                }
            val step = WordMatchStepResponse(stepIndex = 0, pairs = pairs)
            return WordMatchSessionResponse(sessionId = UUID.randomUUID().toString(), steps = listOf(step))
        }
    }
