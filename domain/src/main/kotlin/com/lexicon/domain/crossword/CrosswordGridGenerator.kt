package com.lexicon.domain.crossword

import com.lexicon.domain.dictation.Word
import com.lexicon.interactors.crossword.CrosswordDirection

internal data class PlacedWord(
    val word: Word,
    val row: Int,
    val col: Int,
    val direction: CrosswordDirection,
)

internal data class CrosswordLayout(
    val placements: List<PlacedWord>,
    val rowCount: Int,
    val colCount: Int,
)

/**
 * Greedy crossword layout: places the longest word first, then tries to intersect each
 * subsequent word with an already-placed word via a shared letter. A word with no possible
 * intersection is placed on its own row instead, so every requested word still appears.
 */
internal object CrosswordGridGenerator {
    fun generate(words: List<Word>): CrosswordLayout {
        if (words.isEmpty()) return CrosswordLayout(emptyList(), rowCount = 0, colCount = 0)

        val ordered = words.sortedByDescending { it.text.length }
        val grid = mutableMapOf<Cell, Char>()
        val placed = mutableListOf<PlacedWord>()

        ordered.forEachIndexed { index, word ->
            val letters = word.text.uppercase()
            val placement = if (index == 0) {
                Placement(row = 0, col = 0, direction = CrosswordDirection.ACROSS)
            } else {
                findIntersection(letters, placed, grid) ?: fallbackPlacement(grid)
            }
            place(word, letters, placement, grid, placed)
        }

        return normalize(placed)
    }

    private data class Cell(val row: Int, val col: Int)

    private data class Placement(val row: Int, val col: Int, val direction: CrosswordDirection)

    private fun findIntersection(
        letters: String,
        placed: List<PlacedWord>,
        grid: Map<Cell, Char>,
    ): Placement? {
        for (existing in placed) {
            val existingLetters = existing.word.text.uppercase()
            for (i in letters.indices) {
                for (j in existingLetters.indices) {
                    if (letters[i] != existingLetters[j]) continue
                    val newDirection = if (existing.direction == CrosswordDirection.ACROSS) {
                        CrosswordDirection.DOWN
                    } else {
                        CrosswordDirection.ACROSS
                    }
                    val crossCell = cellAt(existing.row, existing.col, existing.direction, j)
                    val row = if (newDirection == CrosswordDirection.DOWN) crossCell.row - i else crossCell.row
                    val col = if (newDirection == CrosswordDirection.ACROSS) crossCell.col - i else crossCell.col
                    val placement = Placement(row, col, newDirection)
                    if (canPlace(letters, placement, grid)) return placement
                }
            }
        }
        return null
    }

    private fun canPlace(
        letters: String,
        placement: Placement,
        grid: Map<Cell, Char>,
    ): Boolean {
        // The cells immediately before/after the word must be empty, so two words never run together.
        val before = stepBefore(placement)
        val after = cellAt(placement.row, placement.col, placement.direction, letters.length)
        if (grid.containsKey(before) || grid.containsKey(after)) return false

        return letters.withIndex().all { (i, letter) ->
            val cell = cellAt(placement.row, placement.col, placement.direction, i)
            val existing = grid[cell]
            existing == null || existing == letter
        }
    }

    private fun place(
        word: Word,
        letters: String,
        placement: Placement,
        grid: MutableMap<Cell, Char>,
        placed: MutableList<PlacedWord>,
    ) {
        letters.forEachIndexed { i, letter ->
            grid[cellAt(placement.row, placement.col, placement.direction, i)] = letter
        }
        placed += PlacedWord(word, placement.row, placement.col, placement.direction)
    }

    /** Always below every previously placed cell, so it can never collide. */
    private fun fallbackPlacement(grid: Map<Cell, Char>): Placement {
        val row = (grid.keys.maxOfOrNull { it.row } ?: -2) + 2
        return Placement(row, col = 0, direction = CrosswordDirection.ACROSS)
    }

    private fun cellAt(
        row: Int,
        col: Int,
        direction: CrosswordDirection,
        offset: Int,
    ): Cell = if (direction == CrosswordDirection.ACROSS) Cell(row, col + offset) else Cell(row + offset, col)

    private fun stepBefore(placement: Placement): Cell =
        if (placement.direction == CrosswordDirection.ACROSS) {
            Cell(placement.row, placement.col - 1)
        } else {
            Cell(placement.row - 1, placement.col)
        }

    private fun normalize(placed: List<PlacedWord>): CrosswordLayout {
        val cells = placed.flatMap { p ->
            (0 until p.word.text.length).map { offset -> cellAt(p.row, p.col, p.direction, offset) }
        }
        val minRow = cells.minOf { it.row }
        val minCol = cells.minOf { it.col }
        val maxRow = cells.maxOf { it.row }
        val maxCol = cells.maxOf { it.col }
        val shifted = placed.map { it.copy(row = it.row - minRow, col = it.col - minCol) }
        return CrosswordLayout(shifted, rowCount = maxRow - minRow + 1, colCount = maxCol - minCol + 1)
    }
}
