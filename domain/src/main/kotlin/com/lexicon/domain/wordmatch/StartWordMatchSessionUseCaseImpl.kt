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
            val steps =
                (0 until request.stepCount).map { stepIndex ->
                    val pairs =
                        vocabularyRepository.getRandomItems(request.pairsPerStep).map { item ->
                            WordMatchPairResponse(vocabularyItemId = item.id, word = item.text, translation = item.translation)
                        }
                    WordMatchStepResponse(stepIndex = stepIndex, pairs = pairs)
                }
            return WordMatchSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }
    }
