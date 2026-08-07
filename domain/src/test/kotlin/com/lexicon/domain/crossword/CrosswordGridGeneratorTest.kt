package com.lexicon.domain.crossword

import com.lexicon.domain.dictation.Word
import com.lexicon.interactors.crossword.CrosswordDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun word(
    id: Long,
    text: String,
) = Word(id = id, text = text, translation = text, transcription = text)

class CrosswordGridGeneratorTest {
    @Test
    fun `empty word list produces an empty layout`() {
        val layout = CrosswordGridGenerator.generate(emptyList())
        assertEquals(emptyList<PlacedWord>(), layout.placements)
        assertEquals(0, layout.rowCount)
        assertEquals(0, layout.colCount)
    }

    @Test
    fun `single word is placed across at the origin`() {
        val layout = CrosswordGridGenerator.generate(listOf(word(1, "kot")))
        val placement = layout.placements.single()
        assertEquals(0, placement.row)
        assertEquals(0, placement.col)
        assertEquals(CrosswordDirection.ACROSS, placement.direction)
        assertEquals(1, layout.rowCount)
        assertEquals(3, layout.colCount)
    }

    @Test
    fun `every requested word appears exactly once in the layout`() {
        val words = listOf(word(1, "kot"), word(2, "pies"), word(3, "dom"), word(4, "woda"))
        val layout = CrosswordGridGenerator.generate(words)
        assertEquals(words.map { it.id }.toSet(), layout.placements.map { it.word.id }.toSet())
    }

    @Test
    fun `two words sharing a letter are placed crossing at that letter`() {
        val layout = CrosswordGridGenerator.generate(listOf(word(1, "kot"), word(2, "tor")))
        val perWordCells = layout.placements.map { placed ->
            (0 until placed.word.text.length).map { i -> cellFor(placed, i) }.toSet()
        }
        assertTrue("the two words should intersect", perWordCells[0].intersect(perWordCells[1]).isNotEmpty())
    }

    /**
     * The core crossword invariant: reading the finished grid in either direction must only ever
     * spell the puzzle's own answers. Words placed parallel-and-adjacent would create extra letter
     * runs that aren't answers at all, which is what makes a grid look like a blob instead of a
     * crossword.
     */
    @Test
    fun `every maximal letter run in the grid is one of the placed words`() {
        val vocabulary = listOf(
            "kot", "pies", "dom", "woda", "chleb", "mleko", "szkoła", "praca",
            "miasto", "łóżko", "słońce", "przyjaciel", "okno", "stół", "ryba",
        )
        // Many different subsets, since layout quality depends heavily on which letters are available.
        repeat(50) { seed ->
            val words = vocabulary.shuffled(kotlin.random.Random(seed.toLong())).take(8)
                .mapIndexed { index, text -> word(index.toLong(), text) }
            val layout = CrosswordGridGenerator.generate(words)

            val grid = mutableMapOf<Pair<Int, Int>, Char>()
            layout.placements.forEach { placed ->
                placed.word.text.uppercase().forEachIndexed { i, letter -> grid[cellFor(placed, i)] = letter }
            }
            val acrossRuns = layout.placements
                .filter { it.direction == CrosswordDirection.ACROSS }
                .map { Triple(it.row, it.col, it.word.text.length) }
                .toSet()
            val downRuns = layout.placements
                .filter { it.direction == CrosswordDirection.DOWN }
                .map { Triple(it.row, it.col, it.word.text.length) }
                .toSet()

            maximalRuns(grid, horizontal = true).forEach { run ->
                assertTrue("seed $seed: horizontal run $run is not a placed word", run in acrossRuns)
            }
            maximalRuns(grid, horizontal = false).forEach { run ->
                assertTrue("seed $seed: vertical run $run is not a placed word", run in downRuns)
            }
        }
    }

    /** Maximal runs of 2+ adjacent letters, as (row, col, length) of their starting cell. */
    private fun maximalRuns(
        grid: Map<Pair<Int, Int>, Char>,
        horizontal: Boolean,
    ): List<Triple<Int, Int, Int>> {
        if (grid.isEmpty()) return emptyList()
        val runs = mutableListOf<Triple<Int, Int, Int>>()
        val outer = if (horizontal) grid.keys.map { it.first } else grid.keys.map { it.second }
        val inner = if (horizontal) grid.keys.map { it.second } else grid.keys.map { it.first }
        for (o in outer.min()..outer.max()) {
            var i = inner.min()
            while (i <= inner.max()) {
                val cell = if (horizontal) o to i else i to o
                if (grid.containsKey(cell)) {
                    val start = i
                    while (grid.containsKey(if (horizontal) o to (i + 1) else (i + 1) to o)) i++
                    val length = i - start + 1
                    if (length >= 2) runs += if (horizontal) Triple(o, start, length) else Triple(start, o, length)
                }
                i++
            }
        }
        return runs
    }

    @Test
    fun `words with no shared letters are still both placed, without overlapping`() {
        val layout = CrosswordGridGenerator.generate(listOf(word(1, "kot"), word(2, "pies")))
        val cellSets = layout.placements.map { placed ->
            (0 until placed.word.text.length).map { i -> cellFor(placed, i) }.toSet()
        }
        assertTrue(cellSets[0].intersect(cellSets[1]).isEmpty())
    }

    @Test
    fun `layout is normalized so the minimum row and column are both zero`() {
        val layout = CrosswordGridGenerator.generate(listOf(word(1, "kot"), word(2, "tor"), word(3, "pies")))
        val cells = layout.placements.flatMap { placed ->
            (0 until placed.word.text.length).map { i -> cellFor(placed, i) }
        }
        assertEquals(0, cells.minOf { it.first })
        assertEquals(0, cells.minOf { it.second })
        assertEquals(layout.rowCount, cells.maxOf { it.first } + 1)
        assertEquals(layout.colCount, cells.maxOf { it.second } + 1)
    }

    private fun cellFor(
        placed: PlacedWord,
        offset: Int,
    ): Pair<Int, Int> =
        if (placed.direction == CrosswordDirection.ACROSS) {
            placed.row to (placed.col + offset)
        } else {
            (placed.row + offset) to placed.col
        }
}
