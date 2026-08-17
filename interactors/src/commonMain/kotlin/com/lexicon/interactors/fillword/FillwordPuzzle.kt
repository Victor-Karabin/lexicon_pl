package com.lexicon.interactors.fillword

import kotlinx.collections.immutable.ImmutableList

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

    val end: FillwordCell get() = cells.last()
}

data class FillwordPuzzle(
    val grid: ImmutableList<ImmutableList<String>>,
    val words: ImmutableList<FillwordWord>,
) {
    val size: Int get() = grid.size

    fun letterAt(cell: FillwordCell): String = grid.getOrNull(cell.row)?.getOrNull(cell.column).orEmpty()

    /** The word running between two tapped corners, if one does. */
    fun wordBetween(
        from: FillwordCell,
        to: FillwordCell,
    ): FillwordWord? =
        words.firstOrNull {
            (it.start == from && it.end == to) || (it.start == to && it.end == from)
        }
}
