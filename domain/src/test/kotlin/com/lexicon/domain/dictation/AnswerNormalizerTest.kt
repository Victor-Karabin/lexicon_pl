package com.lexicon.domain.dictation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerNormalizerTest {
    private val normalizer = AnswerNormalizer()

    @Test
    fun `matches is case-insensitive`() {
        assertTrue(normalizer.matches(expected = "Kot", submitted = "kot"))
        assertTrue(normalizer.matches(expected = "kot", submitted = "KOT"))
    }

    @Test
    fun `matches ignores leading and trailing whitespace`() {
        assertTrue(normalizer.matches(expected = "kot", submitted = "  kot  "))
    }

    @Test
    fun `matches requires diacritics to match exactly`() {
        assertFalse(normalizer.matches(expected = "łódź", submitted = "lodz"))
        assertTrue(normalizer.matches(expected = "łódź", submitted = "łódź"))
    }

    @Test
    fun `matches is false for a different word`() {
        assertFalse(normalizer.matches(expected = "kot", submitted = "pies"))
    }
}
