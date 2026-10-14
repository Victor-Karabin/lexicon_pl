package com.lexicon.presentation.numbers

import com.lexicon.interactors.numbers.NumberKind
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.presentation.R
import com.lexicon.presentation.common.TextResource
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberLabelTest {
    private fun ordinal(value: Long) = NumberQuestion.of(value, NumberKind.ORDINAL).label()

    @Test
    fun `an ordinal takes the English suffix of its last digits`() {
        assertEquals(TextResource(R.string.numbers_ordinal_st, listOf("1")), ordinal(1))
        assertEquals(TextResource(R.string.numbers_ordinal_nd, listOf("22")), ordinal(22))
        assertEquals(TextResource(R.string.numbers_ordinal_rd, listOf("103")), ordinal(103))
        assertEquals(TextResource(R.string.numbers_ordinal_th, listOf("11")), ordinal(11))
        assertEquals(TextResource(R.string.numbers_ordinal_th, listOf("112")), ordinal(112))
        assertEquals(TextResource(R.string.numbers_ordinal_th, listOf("213")), ordinal(213))
        assertEquals(TextResource(R.string.numbers_ordinal_st, listOf("12 341")), ordinal(12_341))
    }

    @Test
    fun `a cardinal is its digits and a list point its English adverb`() {
        assertEquals(TextResource(R.string.numbers_cardinal_label, listOf("2024")), NumberQuestion.of(2_024).label())
        assertEquals(TextResource(R.string.numbers_list_point_2), NumberQuestion.of(2, NumberKind.ENUMERATION).label())
        assertEquals(TextResource(R.string.numbers_list_point_10), NumberQuestion.of(10, NumberKind.ENUMERATION).label())
    }
}
