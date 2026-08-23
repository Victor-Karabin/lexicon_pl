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
