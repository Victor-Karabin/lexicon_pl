package com.lexicon.application.dictation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerNormalizerSpokenTest {
    private val normalizer = AnswerNormalizer()

    @Test
    fun `a sentence read perfectly matches despite missing punctuation`() =
        assertTrue(
            normalizer.matchesSpoken(
                "Gdy wrócę do domu, zadzwonię do ciebie.",
                "gdy wrócę do domu zadzwonię do ciebie",
            ),
        )

    @Test
    fun `extra spacing from the recognizer does not fail a correct reading`() =
        assertTrue(normalizer.matchesSpoken("Mam nowy telefon.", "  Mam   nowy    telefon  "))

    @Test
    fun `a missing word is still wrong`() = assertFalse(normalizer.matchesSpoken("Mam nowy telefon.", "mam telefon"))

    @Test
    fun `a different word is still wrong`() = assertFalse(normalizer.matchesSpoken("Mam nowy telefon.", "mam stary telefon"))

    @Test
    fun `Polish diacritics are not ignored`() = assertFalse(normalizer.matchesSpoken("Otwórz okno.", "otworz okno"))

    @Test
    fun `written answers still hold punctuation against the learner`() =
        assertFalse(normalizer.matches("Mam nowy telefon.", "mam nowy telefon"))
}
