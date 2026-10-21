package com.lexicon.interactors.countries

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.training.DrillCourseProgress
import com.lexicon.interactors.training.SINGLE_ANSWER_MASTERY_STREAK
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlin.random.Random

enum class CountryField { COUNTRY, MAN, WOMAN, CAPITAL }

data class Country(
    val code: String,
    val english: String,
    val polish: List<String>,
    val man: String?,
    val woman: String?,
    val capital: String,
    val capitalEnglish: String,
) {
    val flag: String
        get() = code.uppercase().asIterable().joinToString("") { letter ->
            charArrayOf(FLAG_HIGH_SURROGATE, FLAG_LOW_SURROGATE_A + (letter - 'A')).concatToString()
        }

    val fields: List<CountryField>
        get() = if (man != null && woman != null) CountryField.entries else listOf(CountryField.COUNTRY, CountryField.CAPITAL)

    fun answers(field: CountryField): List<String> =
        when (field) {
            CountryField.COUNTRY -> polish
            CountryField.MAN -> listOfNotNull(man)
            CountryField.WOMAN -> listOfNotNull(woman)
            CountryField.CAPITAL -> listOf(capital)
        }

    private companion object {
        const val FLAG_HIGH_SURROGATE = '\uD83C'
        const val FLAG_LOW_SURROGATE_A = '\uDDE6'
    }
}

data class CountryFieldProgress(
    val code: String,
    val field: CountryField,
    val attempted: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val streak: Int = 0,
) {
    val isMastered: Boolean get() = streak >= MASTERY_STREAK

    companion object {
        const val MASTERY_STREAK = SINGLE_ANSWER_MASTERY_STREAK
    }
}

data class CountryCourse(
    val codes: ImmutableList<String>,
    val progress: DrillCourseProgress,
)

fun interface LoadCountriesUseCase {
    suspend operator fun invoke(): ImmutableList<Country>
}

fun interface LoadCountryChoiceUseCase {
    suspend operator fun invoke(): Set<String>
}

fun interface SaveCountryChoiceUseCase {
    suspend operator fun invoke(codes: Set<String>)
}

fun interface LoadCountryCourseUseCase {
    suspend operator fun invoke(): CountryCourse?
}

interface NextCountryUseCase {
    suspend operator fun invoke(
        asked: Set<String> = emptySet(),
        random: Random = Random.Default,
    ): Country?
}

fun interface SubmitCountryAnswersUseCase {
    suspend operator fun invoke(
        country: Country,
        typed: Map<CountryField, String>,
    ): ImmutableMap<CountryField, AnswerVerdict>
}
