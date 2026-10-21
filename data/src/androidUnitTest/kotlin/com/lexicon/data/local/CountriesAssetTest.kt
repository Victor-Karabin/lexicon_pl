package com.lexicon.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CountriesAssetTest {
    private val countries = parseCountries(File("src/androidMain/assets/$COUNTRIES_ASSET_PATH").readText())

    private val withoutNationality = setOf("AG", "BB", "BA", "CV", "CK", "CI", "XK", "MH", "NU", "KN", "LC", "VC", "SB", "ST", "TT")

    @Test
    fun `the official list has 197 states with distinct two-letter codes`() {
        assertEquals(197, countries.size)
        assertEquals(197, countries.map { it.code }.toSet().size)
        countries.forEach { assertTrue(it.code, Regex("[A-Z]{2}").matches(it.code)) }
    }

    @Test
    fun `a country has both nationalities or neither`() {
        countries.forEach { assertEquals(it.code, it.man == null, it.woman == null) }
    }

    @Test
    fun `exactly fifteen states have no nationality`() {
        assertEquals(withoutNationality, countries.filter { it.man == null }.map { it.code }.toSet())
    }

    @Test
    fun `the answers to learn add up to 758`() {
        assertEquals(758, countries.map { if (it.man == null) 2 else 4 }.sum())
    }

    @Test
    fun `Naoero is the only country with two Polish names and Nauru is the other`() {
        assertEquals(listOf("Naoero", "Nauru"), countries.single { it.code == "NR" }.polish)
        assertEquals(listOf("NR"), countries.filter { it.polish.size > 1 }.map { it.code })
    }

    @Test
    fun `no name, nationality or capital is blank`() {
        countries.forEach {
            assertTrue(it.code, it.english.isNotBlank() && it.polish.isNotEmpty() && it.polish.all(String::isNotBlank))
            assertTrue(it.code, it.capital.isNotBlank() && it.capitalEnglish.isNotBlank())
            assertTrue(it.code, it.man?.isNotBlank() != false && it.woman?.isNotBlank() != false)
        }
    }

    @Test
    fun `the list is sorted by English name`() {
        assertEquals(countries.map { it.english }.sortedBy { it.lowercase() }, countries.map { it.english })
    }

    @Test
    fun `a nationality is read as null when the asset gives none`() {
        val parsed = parseCountries(
            """[{"code":"AG","english":"Antigua and Barbuda","polish":["Antigua i Barbuda"],"man":null,"woman":null,
            |"capital":"Saint John’s","capitalEnglish":"Saint John's","extra":1}]
            """.trimMargin(),
        )
        assertNull(parsed.single().man)
        assertNotNull(parsed.single().capital)
    }
}
