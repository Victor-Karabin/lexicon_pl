package com.lexicon.application.countries

import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.CountryField
import com.lexicon.interactors.countries.CountryFieldProgress
import com.lexicon.interactors.numbers.NumberProgress
import com.lexicon.interactors.training.SINGLE_ANSWER_MASTERY_STREAK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryTest {
    private fun country(
        code: String = "PL",
        polish: List<String> = listOf("Polska"),
        man: String? = "Polak",
        woman: String? = "Polka",
    ) = Country(code, "Poland", polish, man, woman, "Warszawa", "Warsaw")

    @Test
    fun `the flag is the pair of regional indicator letters of the code`() {
        assertEquals("🇵🇱", country("PL").flag)
        assertEquals("🇽🇰", country("XK").flag)
        assertEquals("🇩🇪", country("de").flag)
    }

    @Test
    fun `a country with both nationalities is asked in all four fields in a fixed order`() {
        assertEquals(
            listOf(CountryField.COUNTRY, CountryField.MAN, CountryField.WOMAN, CountryField.CAPITAL),
            country().fields,
        )
    }

    @Test
    fun `a country without a nationality is asked only its name and capital`() {
        val antigua = country("AG", man = null, woman = null)
        assertEquals(listOf(CountryField.COUNTRY, CountryField.CAPITAL), antigua.fields)
        assertEquals(emptyList<String>(), antigua.answers(CountryField.MAN))
        assertEquals(emptyList<String>(), antigua.answers(CountryField.WOMAN))
    }

    @Test
    fun `each field accepts its own spelling and Naoero also accepts Nauru`() {
        val nauru = country("NR", polish = listOf("Naoero", "Nauru"))
        assertEquals(listOf("Naoero", "Nauru"), nauru.answers(CountryField.COUNTRY))
        assertEquals(listOf("Polak"), country().answers(CountryField.MAN))
        assertEquals(listOf("Polka"), country().answers(CountryField.WOMAN))
        assertEquals(listOf("Warszawa"), country().answers(CountryField.CAPITAL))
    }

    @Test
    fun `one right answer masters a field and none does not`() {
        assertTrue(CountryFieldProgress("PL", CountryField.MAN, attempted = 1, correct = 1, streak = 1).isMastered)
        assertFalse(CountryFieldProgress("PL", CountryField.MAN, attempted = 2, correct = 1, incorrect = 1, streak = 0).isMastered)
        assertFalse(CountryFieldProgress("PL", CountryField.MAN).isMastered)
    }

    @Test
    fun `countries and numbers share the single mastery constant`() {
        assertEquals(1, SINGLE_ANSWER_MASTERY_STREAK)
        assertEquals(SINGLE_ANSWER_MASTERY_STREAK, CountryFieldProgress.MASTERY_STREAK)
        assertEquals(SINGLE_ANSWER_MASTERY_STREAK, NumberProgress.MASTERY_STREAK)
    }
}
