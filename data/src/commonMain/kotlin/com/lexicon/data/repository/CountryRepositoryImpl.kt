package com.lexicon.data.repository

import com.lexicon.boundary.CountryBoundary
import com.lexicon.boundary.CountryProgressBoundary
import com.lexicon.boundary.CountryRepository
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.AttemptCounts
import com.lexicon.data.local.COUNTRIES_ASSET_PATH
import com.lexicon.data.local.CountryChoiceEntity
import com.lexicon.data.local.CountryDao
import com.lexicon.data.local.CountryProgressEntity
import com.lexicon.data.local.parseCountries

class CountryRepositoryImpl(
    private val dao: CountryDao,
    private val assets: AssetReader,
) : CountryRepository {
    private val loaded: List<CountryBoundary> by lazy { parseCountries(assets.readText(COUNTRIES_ASSET_PATH)) }

    override suspend fun countries(): List<CountryBoundary> = loaded

    override suspend fun chosen(): List<String>? = dao.chosen().map { it.code }.ifEmpty { null }

    override suspend fun saveChosen(codes: List<String>) = dao.replaceChosen(codes.map(::CountryChoiceEntity))

    override suspend fun progress(): List<CountryProgressBoundary> =
        dao.progress().map {
            CountryProgressBoundary(
                code = it.code,
                field = it.field,
                attempted = it.attempted,
                correct = it.correct,
                incorrect = it.incorrect,
                streak = it.streak,
            )
        }

    override suspend fun recordAttempt(
        code: String,
        field: String,
        isCorrect: Boolean,
    ) {
        val existing = dao.progressFor(code, field)
        val counts = AttemptCounts.of(existing).after(isCorrect)
        dao.save(
            CountryProgressEntity(
                code = code,
                field = field,
                attempted = counts.attempted,
                correct = counts.correct,
                incorrect = counts.incorrect,
                streak = counts.streak,
            ),
        )
    }
}
