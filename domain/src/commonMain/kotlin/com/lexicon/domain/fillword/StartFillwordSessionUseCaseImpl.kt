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
import kotlinx.collections.immutable.toImmutableList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val GRID_SIZE = 10

private const val MAX_WORDS = 8

private const val DIFFICULTY = "MEDIUM"

class StartFillwordSessionUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val generator: FillwordGenerator,
) : StartFillwordSessionUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(): FillwordSessionResult {
        val favourites = vocabulary.getItemsByIds(vocabulary.favouriteWordIds())
        if (favourites.isEmpty()) return FillwordSessionResult.NoFavourites

        val words = favourites
            .map { it.text }
            .filter { it.none(Char::isWhitespace) && it.length in 3..GRID_SIZE }
            .shuffled()
            .take(MAX_WORDS)
        if (words.isEmpty()) return FillwordSessionResult.NoFavourites

        return when (
            val result = generator.generate(
                FillwordRequestBoundary(words = words, gridSize = GRID_SIZE, difficulty = DIFFICULTY),
            )
        ) {
            FillwordResultBoundary.Offline -> FillwordSessionResult.Offline
            is FillwordResultBoundary.Refused -> FillwordSessionResult.Refused(result.reason)
            is FillwordResultBoundary.Generated -> result.toSession()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun FillwordResultBoundary.Generated.toSession(): FillwordSessionResult {
        val grid = this.grid.map { row -> row.map { it.uppercase() }.toImmutableList() }.toImmutableList()

        val placed = placements.mapNotNull { placement ->
            val direction = runCatching { FillwordDirection.valueOf(placement.direction) }.getOrNull()
                ?: return@mapNotNull null
            val word = FillwordWord(
                word = placement.word.uppercase(),
                start = FillwordCell(placement.startRow, placement.startColumn),
                direction = direction,
            )
            word.takeIf { it.readsCorrectlyIn(grid) }
        }

        return if (placed.isEmpty()) {
            FillwordSessionResult.Refused("no word could be found in the grid")
        } else {
            FillwordSessionResult.Ready(
                sessionId = Uuid.random().toString(),
                puzzle = FillwordPuzzle(grid = grid, words = placed.toImmutableList()),
            )
        }
    }
}

/**
 * Whether the letters along the reported path actually spell the word.
 *
 * The generator is asked to check its own placements and often cannot: a word
 * search is fiddly arithmetic, and a plausible-looking answer with one coordinate
 * out sends the learner hunting for something that is not there. Anything that
 * does not read back is dropped rather than shown.
 */
internal fun FillwordWord.readsCorrectlyIn(grid: List<List<String>>): Boolean =
    cells.withIndex().all { (index, cell) ->
        grid.getOrNull(cell.row)?.getOrNull(cell.column) == word[index].toString()
    }
