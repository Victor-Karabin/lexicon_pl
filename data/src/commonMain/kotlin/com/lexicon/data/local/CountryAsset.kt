package com.lexicon.data.local

import com.lexicon.boundary.CountryBoundary
import kotlinx.serialization.Serializable

const val COUNTRIES_ASSET_PATH = "countries.json"

@Serializable
data class CountryAsset(
    val code: String,
    val english: String,
    val polish: List<String>,
    val man: String? = null,
    val woman: String? = null,
    val capital: String,
    val capitalEnglish: String,
)

fun parseCountries(raw: String): List<CountryBoundary> =
    lessonJson.decodeFromString<List<CountryAsset>>(raw).map {
        CountryBoundary(
            code = it.code,
            english = it.english,
            polish = it.polish,
            man = it.man,
            woman = it.woman,
            capital = it.capital,
            capitalEnglish = it.capitalEnglish,
        )
    }
