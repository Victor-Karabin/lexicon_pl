package com.lexicon.data.repository

import com.lexicon.boundary.CountryBoundary
import com.lexicon.boundary.CountryProgressBoundary
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.COUNTRIES_ASSET_PATH
import com.lexicon.data.local.CountryChoiceEntity
import com.lexicon.data.local.CountryDao
import com.lexicon.data.local.CountryProgressEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryRepositoryImplTest {
    private val asset =
        """
        [
          {"code":"PL","english":"Poland","polish":["Polska"],"man":"Polak","woman":"Polka","capital":"Warszawa","capitalEnglish":"Warsaw"},
          {"code":"AG","english":"Antigua and Barbuda","polish":["Antigua i Barbuda"],"man":null,"woman":null,
           "capital":"Saint John’s","capitalEnglish":"Saint John's"}
        ]
        """.trimIndent()

    private val assets = mockk<AssetReader> { every { readText(COUNTRIES_ASSET_PATH) } returns asset }
    private val dao: CountryDao = mockk(relaxed = true)
    private val repository = CountryRepositoryImpl(dao, assets)

    @Test
    fun `the countries are read from the asset once and keep their missing nationalities`() =
        runTest {
            val first = repository.countries()
            val second = repository.countries()

            assertEquals(
                listOf(
                    CountryBoundary("PL", "Poland", listOf("Polska"), "Polak", "Polka", "Warszawa", "Warsaw"),
                    CountryBoundary("AG", "Antigua and Barbuda", listOf("Antigua i Barbuda"), null, null, "Saint John’s", "Saint John's"),
                ),
                first,
            )
            assertEquals(first, second)
            verify(exactly = 1) { assets.readText(COUNTRIES_ASSET_PATH) }
        }

    @Test
    fun `no countries are chosen until they are saved`() =
        runTest {
            coEvery { dao.chosen() } returns emptyList()
            assertNull(repository.chosen())
        }

    @Test
    fun `the chosen codes are read back`() =
        runTest {
            coEvery { dao.chosen() } returns listOf(CountryChoiceEntity("PL"), CountryChoiceEntity("AG"))
            assertEquals(listOf("PL", "AG"), repository.chosen())
        }

    @Test
    fun `a new choice replaces the old one in one step`() =
        runTest {
            repository.saveChosen(listOf("PL", "DE"))
            coVerify(exactly = 1) { dao.replaceChosen(listOf(CountryChoiceEntity("PL"), CountryChoiceEntity("DE"))) }
        }

    @Test
    fun `an empty choice is saved as no rows`() =
        runTest {
            repository.saveChosen(emptyList())
            coVerify(exactly = 1) { dao.replaceChosen(emptyList()) }
        }

    @Test
    fun `a right answer extends the streak and a wrong one resets it`() =
        runTest {
            coEvery { dao.progressFor("PL", "MAN") } returns
                CountryProgressEntity("PL", "MAN", attempted = 3, correct = 2, incorrect = 1, streak = 2)

            repository.recordAttempt("PL", "MAN", isCorrect = true)
            coVerify { dao.save(CountryProgressEntity("PL", "MAN", attempted = 4, correct = 3, incorrect = 1, streak = 3)) }

            repository.recordAttempt("PL", "MAN", isCorrect = false)
            coVerify { dao.save(CountryProgressEntity("PL", "MAN", attempted = 4, correct = 2, incorrect = 2, streak = 0)) }
        }

    @Test
    fun `a first attempt starts the counts`() =
        runTest {
            coEvery { dao.progressFor("AG", "CAPITAL") } returns null
            repository.recordAttempt("AG", "CAPITAL", isCorrect = true)
            coVerify { dao.save(CountryProgressEntity("AG", "CAPITAL", attempted = 1, correct = 1, incorrect = 0, streak = 1)) }
        }

    @Test
    fun `the progress rows are handed over as they are stored`() =
        runTest {
            coEvery { dao.progress() } returns
                listOf(CountryProgressEntity("PL", "COUNTRY", attempted = 2, correct = 1, incorrect = 1, streak = 1))
            assertEquals(
                listOf(CountryProgressBoundary("PL", "COUNTRY", attempted = 2, correct = 1, incorrect = 1, streak = 1)),
                repository.progress(),
            )
        }
}
