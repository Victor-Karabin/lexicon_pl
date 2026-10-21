package com.lexicon.presentation.countries

import com.lexicon.interactors.countries.CountryField
import com.lexicon.presentation.R

internal fun CountryField.labelId(): Int =
    when (this) {
        CountryField.COUNTRY -> R.string.countries_field_country
        CountryField.MAN -> R.string.countries_field_man
        CountryField.WOMAN -> R.string.countries_field_woman
        CountryField.CAPITAL -> R.string.countries_field_capital
    }
