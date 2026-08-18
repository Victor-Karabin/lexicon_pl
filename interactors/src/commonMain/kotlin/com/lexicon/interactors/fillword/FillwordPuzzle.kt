package com.lexicon.interactors.fillword

import kotlinx.collections.immutable.ImmutableList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

data class FillwordCell(
    val row: Int,
    val column: Int,
)

enum class FillwordDirection(
    val rowStep: Int,
    val columnStep: Int,
) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1),
    UP_LEFT(-1, -1),
    UP_RIGHT(-1, 1),
    DOWN_LEFT(1, -1),
    DOWN_RIGHT(1, 1),
}

data class FillwordWord(
    val word: String,
    val start: FillwordCell,
    val direction: FillwordDirection,
) {
    val cells: List<FillwordCell>
        get() = word.indices.map {
            FillwordCell(
                row = start.row + direction.rowStep * it,
                column = start.column + direction.columnStep * it,
            )
        }
}

data class FillwordPuzzle(
    val grid: ImmutableList<ImmutableList<String>>,
    val words: ImmutableList<FillwordWord>,
) {
    val size: Int get() = grid.size

    fun letterAt(cell: FillwordCell): String = grid.getOrNull(cell.row)?.getOrNull(cell.column).orEmpty()

    /**
     * The straight run of cells a drag from one cell to another means.
     *
     * The run is snapped to whichever of the eight directions the drag was closest to,
     * because a finger crossing a diagonal rarely lands on the exact cell the diagonal
     * wants, and a word that will not be claimed unless traced to the pixel is a word
     * the learner cannot find. Runs are clipped at the edge of the grid.
     */
    fun runBetween(
        from: FillwordCell,
        to: FillwordCell,
    ): List<FillwordCell> {
        val rows = to.row - from.row
        val columns = to.column - from.column
        val down = abs(rows)
        val across = abs(columns)
        if (down == 0 && across == 0) return listOf(from)

        val rowStep = if (across > down * DIAGONAL_TOLERANCE) 0 else rows.sign
        val columnStep = if (down > across * DIAGONAL_TOLERANCE) 0 else columns.sign
        val length = when {
            rowStep == 0 -> across
            columnStep == 0 -> down
            else -> max(down, across)
        }

        return (0..length)
            .takeWhile { step ->
                from.row + rowStep * step in 0 until size && from.column + columnStep * step in 0 until size
            }.map { step ->
                FillwordCell(row = from.row + rowStep * step, column = from.column + columnStep * step)
            }
    }

    /**
     * The word spelled by the cells between two corners, read either way round.
     *
     * Matched on the letters rather than on where the word was placed: the learner sees
     * only letters, so a run that spells a listed word is a find no matter which end
     * they started from or whether the same letters also sit somewhere else.
     */
    fun wordAlong(
        from: FillwordCell,
        to: FillwordCell,
    ): FillwordWord? {
        val run = runBetween(from, to)
        if (run.size < 2) return null

        val spelled = run.joinToString("") { letterAt(it) }
        return words.firstOrNull { it.word == spelled || it.word == spelled.reversed() }
    }
}

/** How far off a diagonal a drag may stray before it counts as a straight line instead. */
private const val DIAGONAL_TOLERANCE = 2
