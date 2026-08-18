package com.lexicon.domain.fillword

import com.lexicon.boundary.FillwordGenerator
import com.lexicon.boundary.FillwordPlacementBoundary
import com.lexicon.boundary.FillwordRequestBoundary
import com.lexicon.boundary.FillwordResultBoundary
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

/** Layouts to demand of a randomised placer before believing it. */
private const val LAYOUTS = 200

class StartFillwordSessionUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val generator: FillwordGenerator = mockk()
    private val useCase = StartFillwordSessionUseCaseImpl(vocabulary, generator)

    private val favourites = listOf(
        "kot", "dom", "okno", "stół", "krzesło", "książka", "lampa", "szafa",
        "kwiat", "zegar", "obraz", "dywan", "łóżko", "ściana",
    ).mapIndexed { index, word -> VocabularyItemBoundary(index + 1L, word, "x", "x") }

    private fun givenFavourites() {
        coEvery { vocabulary.favouriteWordIds() } returns favourites.map { it.id }
        coEvery { vocabulary.getItemsByIds(any()) } returns favourites
    }

    /** The letters along a word's own path have to spell it, or it cannot be found. */
    private fun FillwordPuzzle.everyWordReadable(): Boolean =
        words.all { word ->
            word.cells.withIndex().all { (index, cell) -> letterAt(cell) == word.word[index].toString() }
        }

    /**
     * Placement is randomised, so one passing layout proves little — this asks for many.
     */
    @Test
    fun `every word the model failed to place is placed anyway`() =
        runTest {
            givenFavourites()
            var asked: FillwordRequestBoundary? = null
            coEvery { generator.generate(any()) } answers {
                asked = firstArg()
                FillwordResultBoundary.Generated(
                    grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                    placements = emptyList(),
                )
            }

            repeat(LAYOUTS) {
                val session = useCase() as FillwordSessionResult.Ready

                assertEquals(asked!!.words.toSet(), session.puzzle.words.map { it.word }.toSet())
                assertTrue(session.puzzle.everyWordReadable())
            }
        }

    @Test
    fun `a placement pointing at the wrong cells is corrected rather than dropped`() =
        runTest {
            givenFavourites()
            coEvery { generator.generate(any()) } answers {
                val request = firstArg<FillwordRequestBoundary>()
                FillwordResultBoundary.Generated(
                    grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                    placements = request.words.map {
                        FillwordPlacementBoundary(word = it, startRow = 0, startColumn = 0, direction = "RIGHT")
                    },
                )
            }

            val session = useCase() as FillwordSessionResult.Ready

            assertTrue(session.puzzle.everyWordReadable())
        }

    @Test
    fun `a nonsense direction does not lose the word`() =
        runTest {
            givenFavourites()
            coEvery { generator.generate(any()) } answers {
                val request = firstArg<FillwordRequestBoundary>()
                FillwordResultBoundary.Generated(
                    grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                    placements = request.words.map {
                        FillwordPlacementBoundary(word = it, startRow = 0, startColumn = 0, direction = "SIDEWAYS")
                    },
                )
            }

            val session = useCase() as FillwordSessionResult.Ready

            assertTrue(session.puzzle.everyWordReadable())
            assertTrue(session.puzzle.words.size > 1)
        }

    @Test
    fun `a grid of the wrong shape is replaced rather than shown short`() =
        runTest {
            givenFavourites()
            coEvery { generator.generate(any()) } returns FillwordResultBoundary.Generated(
                grid = listOf(listOf("A", "B"), listOf("C")),
                placements = emptyList(),
            )

            val session = useCase() as FillwordSessionResult.Ready

            assertEquals(GRID_SIZE, session.puzzle.size)
            assertTrue(session.puzzle.grid.all { it.size == GRID_SIZE })
            assertTrue(session.puzzle.grid.all { row -> row.all { it.length == 1 } })
            assertTrue(session.puzzle.everyWordReadable())
        }

    @Test
    fun `the hardest setting is the only one asked for`() =
        runTest {
            givenFavourites()
            var asked: FillwordRequestBoundary? = null
            coEvery { generator.generate(any()) } answers {
                asked = firstArg()
                FillwordResultBoundary.Generated(
                    grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                    placements = emptyList(),
                )
            }

            useCase()

            assertEquals("HARD", asked!!.difficulty)
        }

    @Test
    fun `the grid is filled with more than a handful of words`() =
        runTest {
            givenFavourites()
            coEvery { generator.generate(any()) } returns FillwordResultBoundary.Generated(
                grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                placements = emptyList(),
            )

            val session = useCase() as FillwordSessionResult.Ready

            assertTrue(
                "expected the grid to be packed, got ${session.puzzle.words.size} words",
                session.puzzle.words.size >= 8,
            )
        }

    @Test
    fun `a drag that strays off the diagonal still claims the word`() =
        runTest {
            givenFavourites()
            coEvery { generator.generate(any()) } returns FillwordResultBoundary.Generated(
                grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                placements = emptyList(),
            )

            val puzzle = (useCase() as FillwordSessionResult.Ready).puzzle
            val diagonal = puzzle.words.firstOrNull { it.direction.rowStep != 0 && it.direction.columnStep != 0 }
                ?: return@runTest

            val cells = diagonal.cells
            val short = cells.last().let { FillwordCell(it.row - diagonal.direction.rowStep, it.column) }

            assertEquals(diagonal, puzzle.wordAlong(cells.first(), short))
        }

    @Test
    fun `a run is clipped at the edge rather than running off the grid`() =
        runTest {
            givenFavourites()
            coEvery { generator.generate(any()) } returns FillwordResultBoundary.Generated(
                grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                placements = emptyList(),
            )

            val puzzle = (useCase() as FillwordSessionResult.Ready).puzzle

            val run = puzzle.runBetween(FillwordCell(0, 0), FillwordCell(GRID_SIZE + 5, GRID_SIZE + 5))

            assertEquals(GRID_SIZE, run.size)
            assertTrue(run.all { it.row in 0 until GRID_SIZE && it.column in 0 until GRID_SIZE })
        }

    @Test
    fun `a word can be claimed from either end`() =
        runTest {
            givenFavourites()
            coEvery { generator.generate(any()) } returns FillwordResultBoundary.Generated(
                grid = List(GRID_SIZE) { List(GRID_SIZE) { "X" } },
                placements = emptyList(),
            )

            val puzzle = (useCase() as FillwordSessionResult.Ready).puzzle
            val word = puzzle.words.first()
            val cells = word.cells

            assertEquals(word, puzzle.wordAlong(cells.first(), cells.last()))
            assertEquals(word, puzzle.wordAlong(cells.last(), cells.first()))
        }
}
