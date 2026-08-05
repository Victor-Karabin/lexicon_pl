package com.lexicon.pl.domain.dictation

import java.util.Locale
import javax.inject.Inject

/**
 * Implements the Text Answer Normalization rule from
 * "Trainings - common.rtf" §26: case-insensitive, diacritics compared exactly.
 */
class AnswerNormalizer @Inject constructor() {

    fun matches(expected: String, submitted: String): Boolean = normalize(expected) == normalize(submitted)

    private fun normalize(text: String): String = text.trim().lowercase(Locale.ROOT)
}
