package com.lexicon.presentation.countries

import com.lexicon.interactors.countries.CountryField
import com.lexicon.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryLabelsTest {
    @Test
    fun `each field is labelled with its own string`() {
        assertEquals(R.string.countries_field_country, CountryField.COUNTRY.labelId())
        assertEquals(R.string.countries_field_man, CountryField.MAN.labelId())
        assertEquals(R.string.countries_field_woman, CountryField.WOMAN.labelId())
        assertEquals(R.string.countries_field_capital, CountryField.CAPITAL.labelId())
    }
}
