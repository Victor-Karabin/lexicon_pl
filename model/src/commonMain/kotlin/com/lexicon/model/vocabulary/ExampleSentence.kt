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
            return unmarked(trimmed)
        }

        fun editableText(marked: String): String = unmarked(marked).text

        fun of(
            sentence: String,
            word: String,
        ): ExampleSentence {
            val parsed = parse(sentence)
            if (parsed.isBlank || parsed.emphasis.isNotEmpty()) return parsed

            return parsed.copy(emphasis = parsed.text.emphasisFor(word))
        }
    }
}

private const val SHORTEST_STEM = 3

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

private fun unmarked(source: String): ExampleSentence {
    val plain = StringBuilder()
    val emphasis = mutableListOf<IntRange>()

    var cursor = 0
    while (cursor < source.length) {
        val opening = source.indexOf(MARKER, cursor)
        if (opening < 0) break

        val contentStart = opening + MARKER.length
        val closing = source.indexOf(MARKER, contentStart)
        if (closing < 0) break

        plain.append(source, cursor, opening)
        val from = plain.length
        plain.append(source, contentStart, closing)
        if (plain.length > from) emphasis += from until plain.length

        cursor = closing + MARKER.length
    }
    plain.append(source, cursor, source.length)

    return ExampleSentence(text = plain.toString(), emphasis = emphasis.toImmutableList())
}
