package com.lexicon.domain.dictation

class AnswerNormalizer {
    fun matches(
        expected: String,
        submitted: String,
    ): Boolean = normalize(expected) == normalize(submitted)

    private fun normalize(text: String): String = text.trim().lowercase()
}
