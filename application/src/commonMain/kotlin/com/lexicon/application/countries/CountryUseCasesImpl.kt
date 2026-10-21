package com.lexicon.application.countries

import com.lexicon.application.training.nextToPractise
import com.lexicon.boundary.CountryRepository
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.CountryCourse
import com.lexicon.interactors.countries.CountryField
import com.lexicon.interactors.countries.CountryFieldProgress
import com.lexicon.interactors.countries.LoadCountriesUseCase
import com.lexicon.interactors.countries.LoadCountryChoiceUseCase
import com.lexicon.interactors.countries.LoadCountryCourseUseCase
import com.lexicon.interactors.countries.NextCountryUseCase
import com.lexicon.interactors.countries.SaveCountryChoiceUseCase
import com.lexicon.interactors.countries.SubmitCountryAnswersUseCase
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.training.DrillCourseProgress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlin.random.Random

private suspend fun CountryRepository.catalogue(): List<Country> =
    countries().map {
        Country(
            code = it.code,
            english = it.english,
            polish = it.polish,
            man = it.man,
            woman = it.woman,
            capital = it.capital,
            capitalEnglish = it.capitalEnglish,
        )
    }

private suspend fun CountryRepository.chosenCountries(): List<Country>? {
    val codes = chosen()?.toSet() ?: return null
    return catalogue().filter { it.code in codes }.ifEmpty { null }
}

private suspend fun CountryRepository.progressByField(): Map<Pair<String, CountryField>, CountryFieldProgress> =
    progress().mapNotNull {
        val field = CountryField.entries.firstOrNull { field -> field.name == it.field } ?: return@mapNotNull null
        (it.code to field) to
            CountryFieldProgress(
                code = it.code,
                field = field,
                attempted = it.attempted,
                correct = it.correct,
                incorrect = it.incorrect,
                streak = it.streak,
            )
    }.toMap()

class LoadCountriesUseCaseImpl(
    private val countries: CountryRepository,
) : LoadCountriesUseCase {
    override suspend fun invoke(): ImmutableList<Country> = countries.catalogue().toImmutableList()
}

class LoadCountryChoiceUseCaseImpl(
    private val countries: CountryRepository,
) : LoadCountryChoiceUseCase {
    override suspend fun invoke(): Set<String> {
        val chosen = countries.chosen().orEmpty().toSet()
        return countries.catalogue().map { it.code }.filter { it in chosen }.toSet()
    }
}

class SaveCountryChoiceUseCaseImpl(
    private val countries: CountryRepository,
) : SaveCountryChoiceUseCase {
    override suspend fun invoke(codes: Set<String>) {
        countries.saveChosen(countries.catalogue().map { it.code }.filter { it in codes })
    }
}

class LoadCountryCourseUseCaseImpl(
    private val countries: CountryRepository,
) : LoadCountryCourseUseCase {
    override suspend fun invoke(): CountryCourse? {
        val chosen = countries.chosenCountries() ?: return null
        val progress = countries.progressByField()
        val fields = chosen.flatMap { country -> country.fields.map { country.code to it } }
        return CountryCourse(
            codes = chosen.map { it.code }.toImmutableList(),
            progress = DrillCourseProgress(
                items = chosen.size,
                total = fields.size,
                mastered = fields.count { progress[it]?.isMastered == true },
                attempted = fields.count { (progress[it]?.attempted ?: 0) > 0 },
            ),
        )
    }
}

class NextCountryUseCaseImpl(
    private val countries: CountryRepository,
) : NextCountryUseCase {
    override suspend fun invoke(
        asked: Set<String>,
        random: Random,
    ): Country? {
        val chosen = countries.chosenCountries() ?: return null
        val progress = countries.progressByField()
        return chosen.nextToPractise(
            wasAsked = { it.code in asked },
            isMastered = { country -> country.fields.all { progress[country.code to it]?.isMastered == true } },
            attempts = { country -> progress[country.code to CountryField.COUNTRY]?.attempted ?: 0 },
            random = random,
        )
    }
}

class SubmitCountryAnswersUseCaseImpl(
    private val countries: CountryRepository,
) : SubmitCountryAnswersUseCase {
    override suspend fun invoke(
        country: Country,
        typed: Map<CountryField, String>,
    ): ImmutableMap<CountryField, AnswerVerdict> =
        country.fields.associateWith { field ->
            val verdict = LessonAnswerChecker.verdict(country.answers(field), typed[field].orEmpty())
            countries.recordAttempt(country.code, field.name, verdict == AnswerVerdict.CORRECT)
            verdict
        }.toImmutableMap()
}
