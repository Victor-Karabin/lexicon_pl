package com.lexicon.application.fillword

import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordDirection
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordWord
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlin.random.Random

private const val EMPTY = ""

private const val POLISH_LETTERS = "AĄBCĆDEĘFGHIJKLŁMNŃOÓPRSŚTUWYZŹŻ"

private const val CROSSING_WEIGHT = 5

private const val REPEAT_PENALTY = 4

private const val JITTER = 3

private const val FILLER_FROM_WORDS = 7

private const val FILLER_OUT_OF = 10

internal class FillwordGrid(
    private val size: Int,
    private val random: Random,
) {
    private val letters = MutableList(size) { MutableList(size) { EMPTY } }

    private val placed = mutableListOf<FillwordWord>()

    private val directionUse = mutableMapOf<FillwordDirection, Int>()

    private val everyCell = (0 until size).flatMap { row ->
        (0 until size).map { column -> FillwordCell(row, column) }
    }

    fun add(word: String): Boolean {
        val text = word.uppercase()
        if (text.length > size || placed.any { it.word == text }) return false

        val best = everyCell
            .asSequence()
            .flatMap { cell -> FillwordDirection.entries.map { FillwordWord(text, cell, it) } }
            .mapNotNull { candidate -> score(candidate)?.let { candidate to it } }
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: return false

        commit(best)
        return true
    }

    fun toPuzzle(translations: Map<String, String>): FillwordPuzzle {
        fillGaps()
        return FillwordPuzzle(
            grid = letters.map { it.toImmutableList() }.toImmutableList(),
            words = placed.toImmutableList(),
            translations = translations.toImmutableMap(),
        )
    }

    private fun score(candidate: FillwordWord): Int? {
        var crossings = 0

        candidate.cells.forEachIndexed { index, cell ->
            if (cell.row !in 0 until size || cell.column !in 0 until size) return null

            val existing = letters[cell.row][cell.column]
            if (existing != EMPTY) {
                if (existing != candidate.word[index].toString()) return null
                crossings++
            }
        }
        if (crossings == candidate.word.length) return null

        val used = directionUse[candidate.direction] ?: 0
        return crossings * CROSSING_WEIGHT - used * REPEAT_PENALTY + random.nextInt(JITTER)
    }

    private fun commit(word: FillwordWord) {
        word.cells.forEachIndexed { index, cell ->
            letters[cell.row][cell.column] = word.word[index].toString()
        }
        placed += word
        directionUse[word.direction] = (directionUse[word.direction] ?: 0) + 1
    }

    private fun fillGaps() {
        val bag = placed.flatMap { it.word.toList() }

        everyCell.forEach { cell ->
            if (letters[cell.row][cell.column] != EMPTY) return@forEach

            val fromWords = bag.isNotEmpty() && random.nextInt(FILLER_OUT_OF) < FILLER_FROM_WORDS
            letters[cell.row][cell.column] =
                if (fromWords) bag.random(random).toString() else POLISH_LETTERS.random(random).toString()
        }
    }
}
