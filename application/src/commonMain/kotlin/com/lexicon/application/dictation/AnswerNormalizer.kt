package com.lexicon.application.dictation

private val PUNCTUATION = Regex("[^\\p{L}\\p{N}\\s]")

private val RUNS_OF_SPACE = Regex("\\s+")

class AnswerNormalizer {
    fun matches(
        expected: String,
        submitted: String,
    ): Boolean = normalize(expected) == normalize(submitted)

    fun matchesSpoken(
        expected: String,
        submitted: String,
    ): Boolean = spoken(expected) == spoken(submitted)

    private fun normalize(text: String): String = text.trim().lowercase()

    private fun spoken(text: String): String =
        text
            .replace(PUNCTUATION, " ")
            .replace(RUNS_OF_SPACE, " ")
            .trim()
            .lowercase()
}
