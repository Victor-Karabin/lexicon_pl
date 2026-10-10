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
        val typed = normalise(given)
        if (typed.isEmpty()) return AnswerVerdict.EMPTY
        val accepted = expected.map(::normalise)
        return when (match) {
            AnswerMatch.WHOLE -> when {
                typed in accepted -> AnswerVerdict.CORRECT
                accepted.any { lacksOnlyDiacritics(typed, it) } -> AnswerVerdict.ALMOST
                else -> AnswerVerdict.WRONG
            }
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
        return expected.firstOrNull { lacksOnlyDiacritics(typed, normalise(it)) } ?: expected.first()
    }

    private fun contains(
        typed: String,
        key: String,
    ): Boolean =
        " $typed ".contains(" $key ") ||
            (key.all { it.isDigit() } && typed.replace(" ", "").contains(key))

    private fun fold(text: String): String = text.map { diacritics[it] ?: it }.joinToString("")

    private fun lacksOnlyDiacritics(
        typed: String,
        expected: String,
    ): Boolean =
        typed.length == expected.length &&
            typed.indices.all { typed[it] == expected[it] || typed[it] == diacritics[expected[it]] }

    private fun normalise(text: String): String =
        composed.entries
            .fold(text.lowercase()) { folded, (parts, letter) -> folded.replace(parts, letter) }
            .map { if (it.isLetterOrDigit() || it.isWhitespace()) it else ' ' }
            .joinToString("")
            .replace(spaces, " ")
            .trim()
            .let { if (it.all { char -> char.isDigit() || char == ' ' }) it.replace(" ", "") else it }
}
