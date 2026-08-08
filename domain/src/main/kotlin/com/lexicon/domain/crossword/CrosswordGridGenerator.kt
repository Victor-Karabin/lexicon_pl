package com.lexicon.domain.crossword

import com.lexicon.domain.dictation.Word
import com.lexicon.interactors.crossword.CrosswordDirection
import java.util.Locale
import kotlin.random.Random

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

private const val PLACEMENT_ATTEMPTS = 60

internal object CrosswordGridGenerator {
    fun generate(
        words: List<Word>,
        random: Random = Random.Default,
    ): CrosswordLayout {
        if (words.isEmpty()) return CrosswordLayout(emptyList(), rowCount = 0, colCount = 0)

        var best = layOut(words.sortedByDescending { it.text.length })
        repeat(PLACEMENT_ATTEMPTS - 1) {
            if (best.placements.size == words.size) return best
            val candidate = layOut(words.shuffled(random))
            if (candidate.isPreferredOver(best)) best = candidate
        }
        return best
    }

    private fun CrosswordLayout.isPreferredOver(other: CrosswordLayout): Boolean =
        if (placements.size != other.placements.size) {
            placements.size > other.placements.size
        } else {
            rowCount * colCount < other.rowCount * other.colCount
        }

    private fun layOut(ordered: List<Word>): CrosswordLayout {
        val grid = mutableMapOf<Cell, Char>()
        val placed = mutableListOf<PlacedWord>()

        ordered.forEach { word ->
            val letters = word.text.uppercase(Locale.ROOT)
            val placement = if (placed.isEmpty()) {
                Placement(row = 0, col = 0, direction = CrosswordDirection.ACROSS)
            } else {
                bestIntersection(letters, placed, grid)
            }
            placement?.let { place(word, letters, it, grid, placed) }
        }

        return normalize(placed)
    }

    private data class Cell(val row: Int, val col: Int)

    private data class Placement(val row: Int, val col: Int, val direction: CrosswordDirection)

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
        val before = stepBefore(placement)
        val after = cellAt(placement.row, placement.col, placement.direction, letters.length)
        if (grid.containsKey(before) || grid.containsKey(after)) return false

        return letters.withIndex().all { (i, letter) ->
            val cell = cellAt(placement.row, placement.col, placement.direction, i)
            val existing = grid[cell]
            when {
                existing != null -> existing == letter
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
