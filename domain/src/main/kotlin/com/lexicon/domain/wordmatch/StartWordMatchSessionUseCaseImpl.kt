package com.lexicon.domain.wordmatch

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.Word
import com.lexicon.domain.dictation.isPhrase
import com.lexicon.domain.dictation.toWord
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.wordmatch.StartWordMatchSessionRequest
import com.lexicon.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.interactors.wordmatch.WordMatchPairResponse
import com.lexicon.interactors.wordmatch.WordMatchSessionResponse
import com.lexicon.interactors.wordmatch.WordMatchStepResponse
import java.util.UUID
import javax.inject.Inject

private const val POOL_MULTIPLIER = 4

class StartWordMatchSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val stepCountResolver: StepCountResolver,
    ) : StartWordMatchSessionUseCase {
        override suspend fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse {
            val pairCount = stepCountResolver.resolve(request.stepCount)
            val pool = vocabularyRepository.getRandomItems(pairCount * POOL_MULTIPLIER, request.vocabularyIds).map { it.toWord() }

            val pairs = sameContentTypePairs(pool, pairCount).map { word ->
                WordMatchPairResponse(vocabularyItemId = word.id, word = word.text, translation = word.translation)
            }
            val step = WordMatchStepResponse(stepIndex = 0, pairs = pairs)
            return WordMatchSessionResponse(sessionId = UUID.randomUUID().toString(), steps = listOf(step))
        }

        private fun sameContentTypePairs(
            pool: List<Word>,
            pairCount: Int,
        ): List<Word> {
            val (phrases, singleWords) = pool.partition { it.isPhrase }
            val candidates = listOf(singleWords, phrases)
                .filter { it.isNotEmpty() }
                .sortedByDescending { it.size }
            val chosen = candidates.firstOrNull { it.size >= pairCount } ?: candidates.firstOrNull() ?: pool
            return chosen.take(pairCount)
        }
    }
