package com.lexicon.interactors.course

import com.lexicon.model.course.AnswerMatch

enum class AnswerVerdict { CORRECT, ALMOST, WRONG, EMPTY }

object LessonAnswerChecker {
    private val diacritics = mapOf(
        'ą' to 'a',
        'ć' to 'c',
        'ę' to 'e',
        'ł' to 'l',
        'ń' to 'n',
        'ó' to 'o',
        'ś' to 's',
        'ź' to 'z',
        'ż' to 'z',
    )

    private val spaces = Regex("""\s+""")

    private val separators = Regex("""[\s,]+""")

    private val connectors = setOf("i", "z", "ze", "oraz")

    private const val Z = "z"

    private const val ZE = "ze"

    private val openers = setOf(Z, ZE)

    private const val LIST_MARK = "|"

    private const val CLOSE_SHARE = 3

    private val composed = mapOf(
        "a\u0328" to "ą",
        "c\u0301" to "ć",
        "e\u0328" to "ę",
        "n\u0301" to "ń",
        "o\u0301" to "ó",
        "s\u0301" to "ś",
        "z\u0301" to "ź",
        "z\u0307" to "ż",
    )

    fun verdict(
        expected: List<String>,
        given: String,
        match: AnswerMatch = AnswerMatch.WHOLE,
    ): AnswerVerdict {
        if (match == AnswerMatch.SELECTION) {
            val chosen = selection(given)
            return when {
                chosen.isEmpty() -> AnswerVerdict.EMPTY
                expected.any { selection(it) == chosen } -> AnswerVerdict.CORRECT
                else -> AnswerVerdict.WRONG
            }
        }
        if (match == AnswerMatch.ANY_ORDER) {
            val parts = normalise(given.replace(",", " i "))
            return when {
                parts.isEmpty() -> AnswerVerdict.EMPTY
                expected.any { sameList(parts, it, ::normalise) } -> AnswerVerdict.CORRECT
                expected.any { sameList(fold(parts), it) { key -> fold(normalise(key)) } } -> AnswerVerdict.ALMOST
                else -> AnswerVerdict.WRONG
            }
        }
        val typed = normalise(given)
        if (typed.isEmpty()) return AnswerVerdict.EMPTY
        val accepted = expected.map(::normalise)
        return when (match) {
            AnswerMatch.WHOLE -> when {
                typed in accepted -> AnswerVerdict.CORRECT
                accepted.any { differsOnlyInDiacritics(typed, it) } -> AnswerVerdict.ALMOST
                else -> AnswerVerdict.WRONG
            }
            AnswerMatch.EXACT_LETTERS, AnswerMatch.SELECTION, AnswerMatch.ANY_ORDER ->
                if (typed in accepted) AnswerVerdict.CORRECT else AnswerVerdict.WRONG
            AnswerMatch.KEY_WORDS -> when {
                accepted.any { contains(typed, it) } -> AnswerVerdict.CORRECT
                accepted.any { contains(fold(typed), fold(it)) } -> AnswerVerdict.ALMOST
                else -> AnswerVerdict.WRONG
            }
        }
    }

    fun closest(
        expected: List<String>,
        given: String,
    ): String {
        val typed = normalise(given)
        return expected.firstOrNull { differsOnlyInDiacritics(typed, normalise(it)) } ?: expected.first()
    }

    fun selection(text: String): Set<String> = text.split(separators).map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()

    fun written(key: String): String {
        val parts = key.split(LIST_MARK).map { it.trim() }
        val items = parts.drop(1).filter { it.isNotEmpty() }.map { if (it.startsWith("$ZE ")) it else "$Z $it" }
        return when (items.size) {
            0 -> parts.first()
            1 -> "${parts.first()} ${items.single()}"
            else -> "${parts.first()} ${items.dropLast(1).joinToString(", ")} i ${items.last()}"
        }
    }

    private fun sameList(
        typed: String,
        key: String,
        clean: (String) -> String,
    ): Boolean {
        val parts = key.split(LIST_MARK).map(clean)
        val start = parts.first()
        val items = parts.drop(1).filter { it.isNotEmpty() }
        if (start.isNotEmpty() && typed != start && !typed.startsWith("$start ")) return false
        val rest = typed.removePrefix(start).trim().split(' ').filter { it.isNotEmpty() }
        if (items.isEmpty()) return rest.isEmpty()
        if (rest.firstOrNull() !in openers) return false
        val chunks = mutableListOf<Pair<String, MutableList<String>>>()
        rest.forEach { word -> if (word in connectors) chunks += word to mutableListOf() else chunks.last().second += word }
        val given = chunks.filter { it.second.isNotEmpty() }.map { (connector, words) -> connector to words.joinToString(" ") }
        val wanted = items.map { it.removePrefix("$ZE ") }
        if (given.map { it.second }.sorted() != wanted.sorted()) return false
        val needZe = items.filter { it.startsWith("$ZE ") }.map { it.removePrefix("$ZE ") }.toSet()
        return given.none { (connector, item) -> connector == Z && item in needZe }
    }

    fun same(
        first: String,
        second: String,
    ): Boolean = normalise(first).let { it.isNotEmpty() && it == normalise(second) }

    fun wrongLetters(
        expected: String,
        given: String,
    ): Set<Int> {
        if (given.isBlank()) return emptySet()
        val changed = changedLetters(expected, given)
        val letters = expected.count { it.isLetterOrDigit() }
        return changed.takeIf { it.size <= maxOf(1, letters / CLOSE_SHARE) }.orEmpty()
    }

    fun changedLetters(
        expected: String,
        given: String,
    ): Set<Int> {
        val wanted = expected.lowercase()
        val typed = given.lowercase()
        val lengths = Array(wanted.length + 1) { IntArray(typed.length + 1) }
        for (i in wanted.indices.reversed()) {
            for (j in typed.indices.reversed()) {
                lengths[i][j] = if (wanted[i] == typed[j]) lengths[i + 1][j + 1] + 1 else maxOf(lengths[i + 1][j], lengths[i][j + 1])
            }
        }
        val changed = mutableSetOf<Int>()
        var i = 0
        var j = 0
        while (i < wanted.length) {
            when {
                j < typed.length && wanted[i] == typed[j] -> {
                    i++
                    j++
                }
                j < typed.length && lengths[i][j + 1] >= lengths[i + 1][j] -> j++
                else -> {
                    if (wanted[i].isLetterOrDigit()) changed += i
                    i++
                }
            }
        }
        return changed
    }

    private fun contains(
        typed: String,
        key: String,
    ): Boolean =
        " $typed ".contains(" $key ") ||
            (key.all { it.isDigit() } && typed.replace(" ", "").contains(key))

    private fun fold(text: String): String = text.map { diacritics[it] ?: it }.joinToString("")

    private fun differsOnlyInDiacritics(
        typed: String,
        expected: String,
    ): Boolean = fold(typed) == fold(expected)

    private fun normalise(text: String): String =
        composed.entries
            .fold(text.lowercase()) { folded, (parts, letter) -> folded.replace(parts, letter) }
            .map { if (it.isLetterOrDigit() || it.isWhitespace()) it else ' ' }
            .joinToString("")
            .replace(spaces, " ")
            .trim()
            .let { if (it.all { char -> char.isDigit() || char == ' ' }) it.replace(" ", "") else it }
            .ifEmpty { text.replace(spaces, "") }
}
