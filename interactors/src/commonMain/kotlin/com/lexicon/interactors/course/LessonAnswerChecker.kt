package com.lexicon.interactors.course

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

    fun verdict(
        expected: List<String>,
        given: String,
    ): AnswerVerdict {
        val typed = normalise(given)
        if (typed.isEmpty()) return AnswerVerdict.EMPTY
        val accepted = expected.map(::normalise)
        return when {
            typed in accepted -> AnswerVerdict.CORRECT
            accepted.any { lacksOnlyDiacritics(typed, it) } -> AnswerVerdict.ALMOST
            else -> AnswerVerdict.WRONG
        }
    }

    private fun lacksOnlyDiacritics(
        typed: String,
        expected: String,
    ): Boolean =
        typed.length == expected.length &&
            typed.indices.all { typed[it] == expected[it] || typed[it] == diacritics[expected[it]] }

    private fun normalise(text: String): String = text.trim().replace(spaces, " ").lowercase()
}
