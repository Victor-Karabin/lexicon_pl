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
    fun `intersecting words share a letter at the crossing cell`() {
        // "kot" and "praca" share the letter 'a'... use words guaranteed to share a letter: "kot" / "tor".
        val layout = CrosswordGridGenerator.generate(listOf(word(1, "kot"), word(2, "tor")))
        val cells = layout.placements.associateWith { placed ->
            placed.word.text.uppercase().mapIndexed { i, letter ->
                cellFor(placed, i) to letter
            }
        }.values.flatten()
        val grid = mutableMapOf<Pair<Int, Int>, Char>()
        for ((cell, letter) in cells) {
            val existing = grid[cell]
            assertTrue("conflicting letters at $cell", existing == null || existing == letter)
            grid[cell] = letter
        }
        // At least one cell must be shared between the two placements for them to actually intersect.
        val perWordCells = layout.placements.map { placed ->
            (0 until placed.word.text.length).map { i -> cellFor(placed, i) }.toSet()
        }
        assertTrue(perWordCells[0].intersect(perWordCells[1]).isNotEmpty())
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
    fun `layout is normalized so the minimum row and column are zero`() {
        val layout = CrosswordGridGenerator.generate(listOf(word(1, "kot"), word(2, "tor"), word(3, "pies")))
        assertTrue(layout.placements.all { it.row >= 0 && it.col >= 0 })
        assertTrue(layout.placements.any { it.row == 0 } || layout.placements.any { it.col == 0 })
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
