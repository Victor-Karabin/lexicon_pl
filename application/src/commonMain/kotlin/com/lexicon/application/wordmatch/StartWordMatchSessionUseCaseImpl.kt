@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.application.wordmatch

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.wordmatch.StartWordMatchSessionRequest
import com.lexicon.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.interactors.wordmatch.WordMatchPairResponse
import com.lexicon.interactors.wordmatch.WordMatchSessionResponse
import com.lexicon.interactors.wordmatch.WordMatchStepResponse
import com.lexicon.model.vocabulary.Word
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val POOL_MULTIPLIER = 4

class StartWordMatchSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val stepCountResolver: StepCountResolver,
) : StartWordMatchSessionUseCase {
    override suspend fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse {
        val pairCount = stepCountResolver.resolve(request.stepCount)
        val pool = vocabularyRepository.getRandomItems(pairCount * POOL_MULTIPLIER, request.vocabularyIds).map { it }

        val pairs = sameContentTypePairs(pool, pairCount).map { word ->
            WordMatchPairResponse(vocabularyItemId = word.id.value, word = word.text, translation = word.translation)
        }
        val step = WordMatchStepResponse(stepIndex = 0, pairs = pairs)
        return WordMatchSessionResponse(sessionId = Uuid.random().toString(), steps = listOf(step))
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
