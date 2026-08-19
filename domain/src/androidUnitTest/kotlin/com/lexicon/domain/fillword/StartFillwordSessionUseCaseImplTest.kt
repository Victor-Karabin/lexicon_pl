package com.lexicon.domain.fillword

import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordSessionResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GRID_SIZE = 10

/** Layouts to demand of a randomised placer before believing anything about it. */
private const val LAYOUTS = 200

class StartFillwordSessionUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val useCase = StartFillwordSessionUseCaseImpl(vocabulary)

    private val studySet = listOf(
        "kot" to "cat", "dom" to "house", "okno" to "window", "stół" to "table",
        "krzesło" to "chair", "książka" to "book", "lampa" to "lamp", "szafa" to "wardrobe",
        "kwiat" to "flower", "zegar" to "clock", "obraz" to "picture", "dywan" to "rug",
        "łóżko" to "bed", "ściana" to "wall",
    ).mapIndexed { index, (word, meaning) -> VocabularyItemBoundary(index + 1L, word, meaning, "x") }

    private fun givenStudySet() {
        coEvery { vocabulary.studySetWordIds() } returns studySet.map { it.id }
        coEvery { vocabulary.getItemsByIds(any()) } returns studySet
    }

    private suspend fun puzzle(): FillwordPuzzle = (useCase() as FillwordSessionResult.Ready).puzzle

    private fun FillwordPuzzle.everyWordReadable(): Boolean =
        words.all { word ->
            word.cells.withIndex().all { (index, cell) -> letterAt(cell) == word.word[index].toString() }
        }

    private fun FillwordPuzzle.crossings(): Int {
        val counts = mutableMapOf<FillwordCell, Int>()
        words.forEach { word -> word.cells.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        return counts.values.count { it > 1 }
    }

    @Test
    fun `every word is readable along its own path`() =
        runTest {
            givenStudySet()

            repeat(LAYOUTS) {
                assertTrue(puzzle().everyWordReadable())
            }
        }

    @Test
    fun `the grid is square and every cell carries a single letter`() =
        runTest {
            givenStudySet()

            val puzzle = puzzle()

            assertEquals(GRID_SIZE, puzzle.size)
            assertTrue(puzzle.grid.all { row -> row.size == GRID_SIZE && row.all { it.length == 1 } })
        }

    @Test
    fun `the grid is packed rather than showing a handful of words`() =
        runTest {
            givenStudySet()

            repeat(LAYOUTS) {
                val words = puzzle().words.size
                assertTrue("expected a full grid, got $words words", words >= 8)
            }
        }

    /** The complaint that started this: words laid one per line, left to right. */
    @Test
    fun `words are not all laid out in the same direction`() =
        runTest {
            givenStudySet()

            repeat(LAYOUTS) {
                val directions = puzzle().words.map { it.direction }.toSet()
                assertTrue("only used $directions", directions.size >= 4)
            }
        }

    @Test
    fun `diagonals and backwards runs are used, not just across and down`() =
        runTest {
            givenStudySet()

            repeat(LAYOUTS) {
                val directions = puzzle().words.map { it.direction }
                assertTrue(
                    "no diagonal in $directions",
                    directions.any { it.rowStep != 0 && it.columnStep != 0 },
                )
                // Any run heading up or leftwards counts: UP_LEFT is as backwards as LEFT,
                // and with eight directions shared out those two exact ones can go unused.
                assertTrue(
                    "nothing backwards in $directions",
                    directions.any { it.rowStep < 0 || it.columnStep < 0 },
                )
            }
        }

    @Test
    fun `words cross each other instead of each keeping to itself`() =
        runTest {
            givenStudySet()

            repeat(LAYOUTS) {
                val puzzle = puzzle()
                assertTrue("no shared cells in ${puzzle.words.map { it.word }}", puzzle.crossings() > 0)
            }
        }

    @Test
    fun `no word is hidden entirely inside another`() =
        runTest {
            givenStudySet()

            repeat(LAYOUTS) {
                val puzzle = puzzle()
                puzzle.words.forEach { word ->
                    val others = puzzle.words.filterNot { it == word }
                    val swallowed = others.any { other -> word.cells.all { it in other.cells } }
                    assertTrue("${word.word} sits inside another word", !swallowed)
                }
            }
        }

    @Test
    fun `each hidden word keeps its meaning for the clue list`() =
        runTest {
            givenStudySet()

            val puzzle = puzzle()

            puzzle.words.forEach { word ->
                assertTrue("no meaning for ${word.word}", puzzle.translationOf(word).isNotBlank())
            }
        }

    @Test
    fun `a word can be claimed from either end`() =
        runTest {
            givenStudySet()

            val puzzle = puzzle()
            val word = puzzle.words.first()
            val cells = word.cells

            assertEquals(word, puzzle.wordAlong(cells.first(), cells.last()))
            assertEquals(word, puzzle.wordAlong(cells.last(), cells.first()))
        }

    @Test
    fun `a drag that strays off the diagonal still claims the word`() =
        runTest {
            givenStudySet()

            val puzzle = puzzle()
            val diagonal = puzzle.words.first { it.direction.rowStep != 0 && it.direction.columnStep != 0 }
            val cells = diagonal.cells
            val short = cells.last().let { FillwordCell(it.row - diagonal.direction.rowStep, it.column) }

            assertEquals(diagonal, puzzle.wordAlong(cells.first(), short))
        }

    @Test
    fun `a run is clipped at the edge rather than running off the grid`() =
        runTest {
            givenStudySet()

            val run = puzzle().runBetween(FillwordCell(0, 0), FillwordCell(GRID_SIZE + 5, GRID_SIZE + 5))

            assertEquals(GRID_SIZE, run.size)
            assertTrue(run.all { it.row in 0 until GRID_SIZE && it.column in 0 until GRID_SIZE })
        }

    @Test
    fun `no studySet means no puzzle`() =
        runTest {
            coEvery { vocabulary.studySetWordIds() } returns emptyList()
            coEvery { vocabulary.getItemsByIds(any()) } returns emptyList()

            assertEquals(FillwordSessionResult.EmptyStudySet, useCase())
        }
}
