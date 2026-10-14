package com.lexicon.application.numbers

import com.lexicon.interactors.numbers.NumberGender
import com.lexicon.interactors.numbers.NumberKind
import com.lexicon.interactors.numbers.NumberRule
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
    fun `a number shows the rules its words are built by`() {
        assertEquals(listOf(NumberRule.UNITS), PolishNumbers.rules(0))
        assertEquals(listOf(NumberRule.UNITS), PolishNumbers.rules(10))
        assertEquals(listOf(NumberRule.TEENS), PolishNumbers.rules(15))
        assertEquals(listOf(NumberRule.TENS_TO_FORTY, NumberRule.TENS_AND_UNITS), PolishNumbers.rules(21))
        assertEquals(listOf(NumberRule.TENS_FROM_FIFTY), PolishNumbers.rules(90))
        assertEquals(listOf(NumberRule.HUNDREDS, NumberRule.UNITS), PolishNumbers.rules(105))
    }

    @Test
    fun `thousands and millions name the form they take`() {
        assertEquals(listOf(NumberRule.THOUSAND), PolishNumbers.rules(1_000))
        assertEquals(listOf(NumberRule.THOUSANDS_FEW, NumberRule.UNITS), PolishNumbers.rules(2_000))
        assertEquals(listOf(NumberRule.THOUSANDS_MANY, NumberRule.TEENS), PolishNumbers.rules(12_000))
        assertEquals(
            listOf(
                NumberRule.THOUSANDS_MANY,
                NumberRule.TENS_TO_FORTY,
                NumberRule.TENS_AND_UNITS,
                NumberRule.HUNDREDS,
                NumberRule.TENS_FROM_FIFTY,
            ),
            PolishNumbers.rules(21_365),
        )
        assertEquals(listOf(NumberRule.MILLIONS_FEW, NumberRule.UNITS, NumberRule.THOUSAND), PolishNumbers.rules(3_001_000))
    }

    @Test
    fun `ordinals use the ordinal form only for the last part`() {
        assertEquals("pierwszy", PolishNumbers.words(1, NumberKind.ORDINAL))
        assertEquals("piętnasty", PolishNumbers.words(15, NumberKind.ORDINAL))
        assertEquals("dwudziesty", PolishNumbers.words(20, NumberKind.ORDINAL))
        assertEquals("dwudziesty pierwszy", PolishNumbers.words(21, NumberKind.ORDINAL))
        assertEquals("setny", PolishNumbers.words(100, NumberKind.ORDINAL))
        assertEquals("trzechsetny", PolishNumbers.words(300, NumberKind.ORDINAL))
        assertEquals("sto pierwszy", PolishNumbers.words(101, NumberKind.ORDINAL))
        assertEquals("dwieście dwudziesty", PolishNumbers.words(220, NumberKind.ORDINAL))
        assertEquals("tysięczny", PolishNumbers.words(1_000, NumberKind.ORDINAL))
        assertEquals("tysiąc pierwszy", PolishNumbers.words(1_001, NumberKind.ORDINAL))
        assertEquals("dwa tysiące dwudziesty czwarty", PolishNumbers.words(2_024, NumberKind.ORDINAL))
        assertEquals(listOf("tysiąc setny", "jeden tysiąc setny"), PolishNumbers.answers(1_100, NumberKind.ORDINAL))
    }

    @Test
    fun `zero and round thousands above one thousand have no ordinal here`() {
        assertEquals(false, PolishNumbers.hasForm(0, NumberKind.ORDINAL))
        assertEquals(true, PolishNumbers.hasForm(1_000, NumberKind.ORDINAL))
        assertEquals(false, PolishNumbers.hasForm(2_000, NumberKind.ORDINAL))
        assertEquals(false, PolishNumbers.hasForm(1_000_000, NumberKind.ORDINAL))
        assertEquals(true, PolishNumbers.hasForm(1_000_001, NumberKind.ORDINAL))
    }

    @Test
    fun `firstly to tenthly are po with the neuter ordinal`() {
        assertEquals(
            listOf(
                "po pierwsze", "po drugie", "po trzecie", "po czwarte", "po piąte", "po szóste", "po siódme", "po ósme",
                "po dziewiąte", "po dziesiąte",
            ),
            (1L..10L).map { PolishNumbers.words(it, NumberKind.ENUMERATION) },
        )
        assertEquals(listOf(false, true, true, false), listOf(0L, 1L, 10L, 11L).map { PolishNumbers.hasForm(it, NumberKind.ENUMERATION) })
        assertEquals("pɔ ˈdrugʲɛ", PolishNumbers.transcription(2, NumberKind.ENUMERATION))
        assertEquals(listOf(NumberRule.ENUMERATION), PolishNumbers.rules(2, NumberKind.ENUMERATION))
        (1L..10L).forEach { assertTrue("$it", PolishNumbers.transcription(it, NumberKind.ENUMERATION).isNotBlank()) }
    }

    @Test
    fun `an ordinal is transcribed from its words`() {
        assertEquals("dvuˈd͡ʑɛstɨ ˈpjɛrfʂɨ", PolishNumbers.transcription(21, NumberKind.ORDINAL))
        (1L..20_000L step 13).filter { PolishNumbers.hasForm(it, NumberKind.ORDINAL) }.forEach {
            assertTrue("$it", PolishNumbers.transcription(it, NumberKind.ORDINAL).isNotBlank())
        }
    }

    @Test
    fun `an ordinal shows the ordinal rules`() {
        val genders = NumberRule.ORDINAL_GENDERS
        assertEquals(
            listOf(genders, NumberRule.ORDINAL_TENS, NumberRule.ORDINAL_TENS_AND_UNITS),
            PolishNumbers.rules(21, NumberKind.ORDINAL),
        )
        assertEquals(listOf(genders, NumberRule.ORDINAL_HUNDREDS), PolishNumbers.rules(300, NumberKind.ORDINAL))
        assertEquals(listOf(genders, NumberRule.ORDINAL_THOUSAND), PolishNumbers.rules(1_000, NumberKind.ORDINAL))
        assertEquals(
            listOf(
                genders,
                NumberRule.ORDINAL_LAST_PART,
                NumberRule.THOUSANDS_FEW,
                NumberRule.UNITS,
                NumberRule.ORDINAL_TENS,
                NumberRule.ORDINAL_TENS_AND_UNITS,
            ),
            PolishNumbers.rules(2_024, NumberKind.ORDINAL),
        )
    }

    @Test
    fun `ordinals take -a for feminine and -e for neuter in every ordinal word`() {
        val feminine = NumberGender.FEMININE
        val neuter = NumberGender.NEUTER
        assertEquals("pierwsza", PolishNumbers.words(1, NumberKind.ORDINAL, feminine))
        assertEquals("druga", PolishNumbers.words(2, NumberKind.ORDINAL, feminine))
        assertEquals("trzecia", PolishNumbers.words(3, NumberKind.ORDINAL, feminine))
        assertEquals("drugie", PolishNumbers.words(2, NumberKind.ORDINAL, neuter))
        assertEquals("trzecie", PolishNumbers.words(3, NumberKind.ORDINAL, neuter))
        assertEquals("dwudziesta pierwsza", PolishNumbers.words(21, NumberKind.ORDINAL, feminine))
        assertEquals("sto dwudzieste drugie", PolishNumbers.words(122, NumberKind.ORDINAL, neuter))
        assertEquals("dwa tysiące dwudziesta czwarta", PolishNumbers.words(2_024, NumberKind.ORDINAL, feminine))
        assertEquals("tysięczne", PolishNumbers.words(1_000, NumberKind.ORDINAL, neuter))
        assertEquals("ˈdruga", PolishNumbers.transcription(2, NumberKind.ORDINAL, feminine))
        assertEquals("dvuˈd͡ʑɛsta ˈpjɛrfʂa", PolishNumbers.transcription(21, NumberKind.ORDINAL, feminine))
        (1L..2_000L step 7).filter { PolishNumbers.hasForm(it, NumberKind.ORDINAL) }.forEach { value ->
            NumberGender.entries.forEach {
                assertTrue(
                    "$value $it",
                    PolishNumbers.transcription(value, NumberKind.ORDINAL, it).isNotBlank(),
                )
            }
        }
    }

    @Test
    fun `1 and numbers ending in 2 change with the gender, other cardinals do not`() {
        assertEquals(listOf("jeden", "jedna", "jedno"), NumberGender.entries.map { PolishNumbers.words(1, NumberKind.CARDINAL, it) })
        assertEquals(listOf("dwa", "dwie", "dwa"), NumberGender.entries.map { PolishNumbers.words(2, NumberKind.CARDINAL, it) })
        assertEquals("dwadzieścia dwie", PolishNumbers.words(22, NumberKind.CARDINAL, NumberGender.FEMININE))
        assertEquals("dwieście dwie", PolishNumbers.words(202, NumberKind.CARDINAL, NumberGender.FEMININE))
        assertEquals("ˈjɛdna", PolishNumbers.transcription(1, NumberKind.CARDINAL, NumberGender.FEMININE))
        assertEquals("dvaˈd͡ʑɛɕt͡ɕa dvjɛ", PolishNumbers.transcription(22, NumberKind.CARDINAL, NumberGender.FEMININE))
        assertEquals(
            listOf(true, true, false, false, true, true, false, false),
            listOf(1L, 2L, 3L, 12L, 22L, 1_002L, 2_000L, 21L).map { PolishNumbers.hasGenders(it, NumberKind.CARDINAL) },
        )
        assertEquals(false, PolishNumbers.hasGenders(2, NumberKind.ENUMERATION))
        assertEquals(listOf(NumberRule.ONE_GENDERS, NumberRule.UNITS), PolishNumbers.rules(1))
        assertEquals(NumberRule.TWO_GENDERS, PolishNumbers.rules(22).first())
    }

    @Test
    fun `big numbers are shown in groups of three`() {
        assertEquals("2024", PolishNumbers.digits(2_024))
        assertEquals("12 345", PolishNumbers.digits(12_345))
        assertEquals("1 250 000", PolishNumbers.digits(1_250_000))
    }
}
