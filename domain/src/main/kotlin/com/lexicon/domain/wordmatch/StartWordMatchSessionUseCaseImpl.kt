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

/** Oversized so a single content type still yields enough pairs to fill the board. */
private const val POOL_MULTIPLIER = 4

class StartWordMatchSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val stepCountResolver: StepCountResolver,
    ) : StartWordMatchSessionUseCase {
        override suspend fun invoke(request: StartWordMatchSessionRequest): WordMatchSessionResponse {
            val pairCount = stepCountResolver.resolve(request.stepCount)
            val pool = vocabularyRepository.getRandomItems(pairCount * POOL_MULTIPLIER).map { it.toWord() }

            val pairs = sameContentTypePairs(pool, pairCount).map { word ->
                WordMatchPairResponse(vocabularyItemId = word.id, word = word.text, translation = word.translation)
            }
            val step = WordMatchStepResponse(stepIndex = 0, pairs = pairs)
            return WordMatchSessionResponse(sessionId = UUID.randomUUID().toString(), steps = listOf(step))
        }

        /**
         * A board mixing single words with multi-word phrases reads badly — the phrases are far wider
         * than the words and stand out as obviously-matching pairs. Pick whichever content type can
         * fill the board, preferring the larger group so the board is as full as possible.
         */
        private fun sameContentTypePairs(
            pool: List<Word>,
            pairCount: Int,
        ): List<Word> {
            val (phrases, singleWords) = pool.partition { it.isPhrase }
            val candidates = listOf(singleWords, phrases)
                .filter { it.isNotEmpty() }
                .sortedByDescending { it.size }
            // Falls back to the pool as-is only when the vocabulary is too small to fill a board
            // from one type, which is better than returning fewer pairs than asked for.
            val chosen = candidates.firstOrNull { it.size >= pairCount } ?: candidates.firstOrNull() ?: pool
            return chosen.take(pairCount)
        }
    }
