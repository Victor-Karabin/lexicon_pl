package com.lexicon.application.numbers

import com.lexicon.interactors.numbers.NumberSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberSelectionTest {
    @Test
    fun `numbers and ranges separated by commas become one sorted list`() {
        assertEquals(
            NumberSelection.Valid(listOf(1L, 2L, 3L, 35L, 100L, 101L, 102L)),
            NumberSelection.parse("1-3, 35, 100–102"),
        )
    }

    @Test
    fun `repeats are counted once and a backwards range still works`() {
        assertEquals(NumberSelection.Valid(listOf(5L, 6L, 7L)), NumberSelection.parse("7-5, 6, 5"))
    }

    @Test
    fun `a space inside a big number groups its digits`() {
        assertEquals(NumberSelection.Valid(listOf(12_000L, 12_001L)), NumberSelection.parse("12 000 - 12 001"))
        assertEquals(NumberSelection.Valid(listOf(1_000_000L)), NumberSelection.parse("1 000 000"))
    }

    @Test
    fun `semicolons and new lines also separate`() {
        assertEquals(NumberSelection.Valid(listOf(1L, 2L, 3L)), NumberSelection.parse("1;2\n3"))
    }

    @Test
    fun `an empty input asks for numbers`() {
        assertEquals(NumberSelection.Empty, NumberSelection.parse(" , "))
    }

    @Test
    fun `a part that is not a number or a range is named`() {
        assertEquals(NumberSelection.Unreadable("abc"), NumberSelection.parse("1-5, abc"))
        assertEquals(NumberSelection.Unreadable("-3"), NumberSelection.parse("-3"))
        assertEquals(NumberSelection.Unreadable("1-20 2024"), NumberSelection.parse("1-20 2024"))
        assertEquals(NumberSelection.Unreadable("5 7"), NumberSelection.parse("5 7"))
    }

    @Test
    fun `numbers beyond the largest one are refused`() {
        assertEquals(NumberSelection.TooLarge, NumberSelection.parse("1000000000"))
        assertEquals(NumberSelection.TooLarge, NumberSelection.parse("99999999999999999999"))
    }

    @Test
    fun `too many numbers are refused`() {
        assertEquals(NumberSelection.TooMany, NumberSelection.parse("1-200000"))
        assertEquals(NumberSelection.TooMany, NumberSelection.parse("1-60000, 100000-160000"))
    }
}
