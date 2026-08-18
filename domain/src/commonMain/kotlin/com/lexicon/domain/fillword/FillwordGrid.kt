package com.lexicon.domain.fillword

import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordDirection
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordWord
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

private const val POLISH_LETTERS = "AĄBCĆDEĘFGHIJKLŁMNŃOÓPRSŚTUWYZŹŻ"

/** Diagonal and backwards first, which is what the hardest setting asks for. */
private val HARD_DIRECTIONS = listOf(
    FillwordDirection.UP_LEFT,
    FillwordDirection.UP_RIGHT,
    FillwordDirection.DOWN_LEFT,
    FillwordDirection.DOWN_RIGHT,
    FillwordDirection.LEFT,
    FillwordDirection.UP,
)

private val PLAIN_DIRECTIONS = listOf(FillwordDirection.RIGHT, FillwordDirection.DOWN)

/**
 * A grid words are written into rather than searched for.
 *
 * The model is asked to build the puzzle, but placing letters on a lattice is
 * arithmetic rather than language, and it drops or misplaces words often enough that
 * a grid taken on trust shows a handful of the words it promised. So its answer is
 * used as the sheet of filler letters and as a first suggestion of where each word
 * goes, and anything it failed to place is placed here. Every word ends up readable
 * because it is written in, not because it was checked.
 */
internal class FillwordGrid(
    private val size: Int,
    seed: List<List<String>>,
    private val random: Random,
) {
    private val letters = MutableList(size) { row ->
        MutableList(size) { column ->
            seed.getOrNull(row)?.getOrNull(column)?.trim()?.uppercase()?.takeIf { it.length == 1 }
                ?: POLISH_LETTERS.random(random).toString()
        }
    }

    private val taken = mutableSetOf<FillwordCell>()
    private val placed = mutableListOf<FillwordWord>()

    private val everyCell = (0 until size).flatMap { row ->
        (0 until size).map { column -> FillwordCell(row, column) }
    }

    /** Puts a word where the model said it goes, if that spot is still free. */
    fun accept(word: FillwordWord): Boolean {
        if (holds(word.word)) return false
        if (!fits(word)) return false
        commit(word)
        return true
    }

    /** Finds a spot for a word the model did not place, hardest orientations first. */
    fun add(word: String): Boolean {
        val text = word.uppercase()
        if (text.length > size || holds(text)) return false

        val spot = candidates(HARD_DIRECTIONS).firstOrNull { fits(it, text) }
            ?: candidates(PLAIN_DIRECTIONS).firstOrNull { fits(it, text) }
            ?: return false

        commit(FillwordWord(text, spot.first, spot.second))
        return true
    }

    fun toPuzzle(): FillwordPuzzle =
        FillwordPuzzle(
            grid = letters.map { it.toImmutableList() }.toImmutableList(),
            words = placed.toImmutableList(),
        )

    private fun holds(word: String): Boolean = placed.any { it.word == word }

    private fun candidates(directions: List<FillwordDirection>): Sequence<Pair<FillwordCell, FillwordDirection>> =
        directions
            .flatMap { direction -> everyCell.map { it to direction } }
            .shuffled(random)
            .asSequence()

    private fun fits(
        spot: Pair<FillwordCell, FillwordDirection>,
        word: String,
    ): Boolean = fits(FillwordWord(word, spot.first, spot.second))

    /**
     * A word fits where every cell is inside the grid and every cell already spoken for
     * by another word carries the letter this word needs there. Free cells hold nothing
     * but filler, so they can be written over.
     */
    private fun fits(word: FillwordWord): Boolean =
        word.cells.withIndex().all { (index, cell) ->
            val inside = cell.row in 0 until size && cell.column in 0 until size
            inside && (cell !in taken || letters[cell.row][cell.column] == word.word[index].toString())
        }

    private fun commit(word: FillwordWord) {
        word.cells.forEachIndexed { index, cell ->
            letters[cell.row][cell.column] = word.word[index].toString()
            taken += cell
        }
        placed += word
    }
}
