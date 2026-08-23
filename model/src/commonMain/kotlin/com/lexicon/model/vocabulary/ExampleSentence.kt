package com.lexicon.model.vocabulary

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val MARKER = "**"

data class ExampleSentence(
    val text: String,
    val emphasis: ImmutableList<IntRange> = persistentListOf(),
) {
    val isBlank: Boolean get() = text.isBlank()

    companion object {
        val None = ExampleSentence("")

        fun parse(marked: String): ExampleSentence {
            val trimmed = marked.trim()
            if (trimmed.isEmpty()) return None

            val plain = StringBuilder()
            val emphasis = mutableListOf<IntRange>()

            var cursor = 0
            while (cursor < trimmed.length) {
                val opening = trimmed.indexOf(MARKER, cursor)
                if (opening < 0) break

                val contentStart = opening + MARKER.length
                val closing = trimmed.indexOf(MARKER, contentStart)
                if (closing < 0) break

                plain.append(trimmed, cursor, opening)
                val from = plain.length
                plain.append(trimmed, contentStart, closing)
                if (plain.length > from) emphasis += from until plain.length

                cursor = closing + MARKER.length
            }
            plain.append(trimmed, cursor, trimmed.length)

            return ExampleSentence(text = plain.toString(), emphasis = emphasis.toImmutableList())
        }

        fun of(
            sentence: String,
            word: String,
        ): ExampleSentence {
            val parsed = parse(sentence)
            if (parsed.isBlank || parsed.emphasis.isNotEmpty()) return parsed

            return parsed.copy(emphasis = parsed.text.emphasisFor(word))
        }

        fun mark(
            text: String,
            emphasis: List<IntRange>,
        ): String {
            if (emphasis.isEmpty()) return text

            val marked = StringBuilder()
            var cursor = 0
            emphasis.sortedBy { it.first }.forEach { range ->
                if (range.first < cursor || range.last >= text.length) return@forEach
                marked.append(text, cursor, range.first).append(MARKER)
                marked.append(text, range.first, range.last + 1).append(MARKER)
                cursor = range.last + 1
            }
            return marked.append(text, cursor, text.length).toString()
        }
    }
}

private const val SHORTEST_STEM = 3

/**
 * Polish inflects, so the sentence rarely spells the word the way the entry does:
 * kobieta turns up as kobietę, zamek as zamku. Matching walks the word back a letter
 * at a time until a token in the sentence starts with what is left, which covers
 * endings but not stems that alternate outright — brać becoming biorę is beyond it,
 * and such a sentence simply reads without emphasis.
 */
private fun String.emphasisFor(word: String): ImmutableList<IntRange> {
    val needle = word.trim().lowercase()
    if (needle.length < SHORTEST_STEM) return persistentListOf()

    val tokens = tokens()
    for (length in needle.length downTo SHORTEST_STEM) {
        val stem = needle.take(length)
        val phrase = tokens.matching(stem, needle, this)
        if (phrase != null) return persistentListOf(phrase)
    }
    return persistentListOf()
}

private fun List<IntRange>.matching(
    stem: String,
    needle: String,
    text: String,
): IntRange? {
    val words = needle.split(' ').filter { it.isNotBlank() }
    val first = indexOfFirst { text.substring(it).lowercase().startsWith(stem) }
    if (first < 0) return null

    // A phrase entry such as "bać się" should light up both of its words.
    val last = (first + words.size - 1).coerceAtMost(lastIndex)
    return this[first].first..this[last].last
}

private fun String.tokens(): List<IntRange> {
    val tokens = mutableListOf<IntRange>()
    var start = -1
    forEachIndexed { index, letter ->
        if (letter.isLetter() || letter == '-') {
            if (start < 0) start = index
        } else if (start >= 0) {
            tokens += start until index
            start = -1
        }
    }
    if (start >= 0) tokens += start until length
    return tokens
}
