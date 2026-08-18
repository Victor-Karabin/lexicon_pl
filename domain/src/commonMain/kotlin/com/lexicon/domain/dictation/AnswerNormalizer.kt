package com.lexicon.domain.dictation

private val PUNCTUATION = Regex("[^\\p{L}\\p{N}\\s]")

private val RUNS_OF_SPACE = Regex("\\s+")

class AnswerNormalizer {
    fun matches(
        expected: String,
        submitted: String,
    ): Boolean = normalize(expected) == normalize(submitted)

    /**
     * Whether something said out loud matches what was written down.
     *
     * Recognition returns bare words: no commas, no full stop, capitals wherever the
     * engine felt like them. Holding a spoken answer to the page's punctuation would
     * fail every sentence read perfectly, so only the words themselves are compared.
     */
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
