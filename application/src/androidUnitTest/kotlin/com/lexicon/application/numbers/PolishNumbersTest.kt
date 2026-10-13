package com.lexicon.application.numbers

import com.lexicon.interactors.numbers.PolishNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishNumbersTest {
    @Test
    fun `units, teens and tens are single words`() {
        assertEquals("zero", PolishNumbers.words(0))
        assertEquals("jeden", PolishNumbers.words(1))
        assertEquals("dwanaście", PolishNumbers.words(12))
        assertEquals("piętnaście", PolishNumbers.words(15))
        assertEquals("dwadzieścia", PolishNumbers.words(20))
        assertEquals("czterdzieści", PolishNumbers.words(40))
        assertEquals("dziewięćdziesiąt", PolishNumbers.words(90))
    }

    @Test
    fun `compound numbers join tens, units and hundreds`() {
        assertEquals("dwadzieścia jeden", PolishNumbers.words(21))
        assertEquals("sto", PolishNumbers.words(100))
        assertEquals("sto pięć", PolishNumbers.words(105))
        assertEquals("dwieście dwanaście", PolishNumbers.words(212))
        assertEquals("pięćset czterdzieści", PolishNumbers.words(540))
        assertEquals("dziewięćset dziewięćdziesiąt dziewięć", PolishNumbers.words(999))
    }

    @Test
    fun `thousands take tysiąc, tysiące or tysięcy`() {
        assertEquals("tysiąc", PolishNumbers.words(1_000))
        assertEquals("tysiąc dwieście pięćdziesiąt", PolishNumbers.words(1_250))
        assertEquals("dwa tysiące dwadzieścia cztery", PolishNumbers.words(2_024))
        assertEquals("pięć tysięcy", PolishNumbers.words(5_000))
        assertEquals("dwanaście tysięcy", PolishNumbers.words(12_000))
        assertEquals("dwadzieścia dwa tysiące", PolishNumbers.words(22_000))
        assertEquals("dwadzieścia jeden tysięcy", PolishNumbers.words(21_000))
        assertEquals("sto jeden tysięcy", PolishNumbers.words(101_000))
    }

    @Test
    fun `millions take milion, miliony or milionów`() {
        assertEquals("milion", PolishNumbers.words(1_000_000))
        assertEquals("trzy miliony", PolishNumbers.words(3_000_000))
        assertEquals("czternaście milionów", PolishNumbers.words(14_000_000))
        assertEquals(
            "dziewięćset dziewięćdziesiąt dziewięć milionów dziewięćset dziewięćdziesiąt dziewięć tysięcy " +
                "dziewięćset dziewięćdziesiąt dziewięć",
            PolishNumbers.words(PolishNumbers.MAX),
        )
    }

    @Test
    fun `a leading one may be said aloud`() {
        assertEquals(listOf("tysiąc sto", "jeden tysiąc sto"), PolishNumbers.answers(1_100))
        assertEquals(listOf("dwadzieścia"), PolishNumbers.answers(20))
    }

    @Test
    fun `the transcription follows the words`() {
        assertEquals("dvaˈd͡ʑɛɕt͡ɕa ˈjɛdɛn", PolishNumbers.transcription(21))
        assertEquals("pjɛɲˈd͡ʑɛɕɔnt", PolishNumbers.transcription(50))
        assertEquals("dva tɨˈɕɔnt͡sɛ", PolishNumbers.transcription(2_000))
    }

    @Test
    fun `every number up to a hundred thousand has a transcription`() {
        (0L..100_000L step 7).forEach { assertTrue("$it", PolishNumbers.transcription(it).isNotBlank()) }
    }

    @Test
    fun `big numbers are shown in groups of three`() {
        assertEquals("2024", PolishNumbers.digits(2_024))
        assertEquals("12 345", PolishNumbers.digits(12_345))
        assertEquals("1 250 000", PolishNumbers.digits(1_250_000))
    }
}
