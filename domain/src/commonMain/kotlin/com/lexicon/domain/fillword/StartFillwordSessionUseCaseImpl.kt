package com.lexicon.domain.fillword

import com.lexicon.boundary.FillwordGenerator
import com.lexicon.boundary.FillwordRequestBoundary
import com.lexicon.boundary.FillwordResultBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordDirection
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordSessionResult
import com.lexicon.interactors.fillword.FillwordWord
import com.lexicon.interactors.fillword.StartFillwordSessionUseCase
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val GRID_SIZE = 10

private const val MAX_WORDS = 12

private const val MIN_WORD_LENGTH = 3

/**
 * How many letters of the grid the hidden words may claim before we stop adding more.
 *
 * Words overlap, so this is a floor on how full the grid gets rather than a ceiling —
 * it keeps the placer from being handed more than it can fit.
 */
private const val MAX_LETTERS = GRID_SIZE * GRID_SIZE / 2

private const val DIFFICULTY = "HARD"

/**
 * How many times to lay the whole grid out before settling for the best attempt.
 *
 * Placement is greedy, so an unlucky early word can block a later one; laying it out
 * again from a fresh shuffle almost always finds room. It is arithmetic on a hundred
 * cells, so trying several times costs nothing worth measuring.
 */
private const val PACKING_ATTEMPTS = 12

class StartFillwordSessionUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val generator: FillwordGenerator,
) : StartFillwordSessionUseCase {
    override suspend fun invoke(): FillwordSessionResult {
        val favourites = vocabulary.getItemsByIds(vocabulary.favouriteWordIds())
        if (favourites.isEmpty()) return FillwordSessionResult.NoFavourites

        val words = favourites
            .map { it.text.uppercase() }
            .filter { it.none(Char::isWhitespace) && it.length in MIN_WORD_LENGTH..GRID_SIZE }
            .distinct()
            .shuffled()
            .fillGrid()
        if (words.isEmpty()) return FillwordSessionResult.NoFavourites

        return when (
            val result = generator.generate(
                FillwordRequestBoundary(words = words, gridSize = GRID_SIZE, difficulty = DIFFICULTY),
            )
        ) {
            FillwordResultBoundary.Offline -> FillwordSessionResult.Offline
            is FillwordResultBoundary.Refused -> FillwordSessionResult.Refused(result.reason)
            is FillwordResultBoundary.Generated -> result.toSession(words)
        }
    }

    /** As many words as the grid will hold, taken in the order they were shuffled into. */
    private fun List<String>.fillGrid(): List<String> {
        var letters = 0
        return takeWhile { word ->
            letters += word.length
            letters <= MAX_LETTERS
        }.take(MAX_WORDS)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun FillwordResultBoundary.Generated.toSession(words: List<String>): FillwordSessionResult {
        var best = pack(words)
        repeat(PACKING_ATTEMPTS - 1) {
            if (best.words.size < words.size) {
                val attempt = pack(words)
                if (attempt.words.size > best.words.size) best = attempt
            }
        }

        return if (best.words.isEmpty()) {
            FillwordSessionResult.Refused("no word would fit the grid")
        } else {
            FillwordSessionResult.Ready(sessionId = Uuid.random().toString(), puzzle = best)
        }
    }

    /**
     * One complete layout: the model's own placements where they still work, then every
     * remaining word, longest first because long words are the ones that run out of room.
     */
    private fun FillwordResultBoundary.Generated.pack(words: List<String>): FillwordPuzzle {
        val builder = FillwordGrid(size = GRID_SIZE, seed = grid, random = Random.Default)

        placements.forEach { placement ->
            val direction = runCatching { FillwordDirection.valueOf(placement.direction) }.getOrNull()
                ?: return@forEach
            val word = placement.word.uppercase()
            if (word in words) {
                builder.accept(
                    FillwordWord(
                        word = word,
                        start = FillwordCell(placement.startRow, placement.startColumn),
                        direction = direction,
                    ),
                )
            }
        }

        words.sortedByDescending { it.length }.forEach(builder::add)
        return builder.toPuzzle()
    }
}
