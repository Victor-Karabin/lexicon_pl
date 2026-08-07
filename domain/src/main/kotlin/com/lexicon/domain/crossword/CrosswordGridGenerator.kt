package com.lexicon.domain.crossword

import com.lexicon.domain.dictation.Word
import com.lexicon.interactors.crossword.CrosswordDirection
import java.util.Locale

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
            val letters = word.text.uppercase(Locale.ROOT)
            val placement = if (index == 0) {
                Placement(row = 0, col = 0, direction = CrosswordDirection.ACROSS)
            } else {
                bestIntersection(letters, placed, grid) ?: fallbackPlacement(grid)
            }
            place(word, letters, placement, grid, placed)
        }

        return normalize(placed)
    }

    private data class Cell(val row: Int, val col: Int)

    private data class Placement(val row: Int, val col: Int, val direction: CrosswordDirection)

    /** Of all legal intersections, picks the one that keeps the overall grid most compact. */
    private fun bestIntersection(
        letters: String,
        placed: List<PlacedWord>,
        grid: Map<Cell, Char>,
    ): Placement? =
        placed.asSequence()
            .flatMap { existing ->
                val existingLetters = existing.word.text.uppercase(Locale.ROOT)
                letters.indices.asSequence().flatMap { i ->
                    existingLetters.indices.asSequence().mapNotNull { j ->
                        if (letters[i] != existingLetters[j]) return@mapNotNull null
                        val newDirection = if (existing.direction == CrosswordDirection.ACROSS) {
                            CrosswordDirection.DOWN
                        } else {
                            CrosswordDirection.ACROSS
                        }
                        val crossCell = cellAt(existing.row, existing.col, existing.direction, j)
                        val row = if (newDirection == CrosswordDirection.DOWN) crossCell.row - i else crossCell.row
                        val col = if (newDirection == CrosswordDirection.ACROSS) crossCell.col - i else crossCell.col
                        Placement(row, col, newDirection)
                    }
                }
            }
            .filter { canPlace(letters, it, grid) }
            .minByOrNull { boundingBoxAreaWith(letters, it, grid) }

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
            when {
                // A shared cell is a legitimate crossing as long as the letters agree.
                existing != null -> existing == letter
                // An unshared cell must not touch another word sideways: two words running parallel
                // and adjacent would form letter sequences that aren't any of the puzzle's answers.
                else -> perpendicularNeighbours(cell, placement.direction).none(grid::containsKey)
            }
        }
    }

    private fun perpendicularNeighbours(
        cell: Cell,
        direction: CrosswordDirection,
    ): List<Cell> =
        if (direction == CrosswordDirection.ACROSS) {
            listOf(Cell(cell.row - 1, cell.col), Cell(cell.row + 1, cell.col))
        } else {
            listOf(Cell(cell.row, cell.col - 1), Cell(cell.row, cell.col + 1))
        }

    private fun boundingBoxAreaWith(
        letters: String,
        placement: Placement,
        grid: Map<Cell, Char>,
    ): Int {
        val cells = grid.keys + letters.indices.map { cellAt(placement.row, placement.col, placement.direction, it) }
        val height = cells.maxOf { it.row } - cells.minOf { it.row } + 1
        val width = cells.maxOf { it.col } - cells.minOf { it.col } + 1
        return height * width
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
