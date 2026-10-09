package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonAnswerCheckerTest {
    private fun verdict(
        expected: String,
        given: String,
    ) = LessonAnswerChecker.verdict(listOf(expected), given)

    @Test
    fun `every key of lesson 1 is accepted as written`() {
        val keys = listOf(
            "cześć", "szkoła", "dziękuję", "przepraszam", "powtórzyć", "sześć", "dziewięć", "gdzie",
            "dobry", "Nazywam", "Bardzo", "mi", "jestem", "Cześć", "Miło", "Mnie",
            "Jestem", "nie", "nazywa", "się",
            "0", "2", "6", "9", "10", "1", "5", "4",
            "trzy", "siedem", "osiem", "zero", "jeden",
        )
        keys.forEach { assertEquals(it, AnswerVerdict.CORRECT, verdict(it, it)) }
    }

    @Test
    fun `letter case and surrounding spaces are ignored`() {
        assertEquals(AnswerVerdict.CORRECT, verdict("Nazywam", "nazywam"))
        assertEquals(AnswerVerdict.CORRECT, verdict("dobry", "  DOBRY "))
        assertEquals(AnswerVerdict.CORRECT, verdict("Cześć", "CZEŚĆ"))
    }

    @Test
    fun `missing diacritics are wrong but almost`() {
        assertEquals(AnswerVerdict.ALMOST, verdict("sześć", "szesc"))
        assertEquals(AnswerVerdict.ALMOST, verdict("dziękuję", "dziekuje"))
        assertEquals(AnswerVerdict.ALMOST, verdict("szkoła", "szkola"))
        assertEquals(AnswerVerdict.ALMOST, verdict("się", "sie"))
    }

    @Test
    fun `a different word is wrong, not almost`() {
        assertEquals(AnswerVerdict.WRONG, verdict("sześć", "cześć"))
        assertEquals(AnswerVerdict.WRONG, verdict("dziewięć", "dzięwięć"))
        assertEquals(AnswerVerdict.WRONG, verdict("sześć", "seść"))
        assertEquals(AnswerVerdict.WRONG, verdict("9", "10"))
        assertEquals(AnswerVerdict.WRONG, verdict("Mnie", "mi"))
    }

    @Test
    fun `a wrong diacritic is not forgiven as almost when the plain letters differ`() {
        assertEquals(AnswerVerdict.WRONG, verdict("powtórzyć", "powturzyć"))
    }

    @Test
    fun `nothing typed is empty rather than wrong`() {
        assertEquals(AnswerVerdict.EMPTY, verdict("trzy", "   "))
    }

    @Test
    fun `any accepted answer counts`() {
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(listOf("dzień dobry", "dobry"), "Dobry"))
    }
}
