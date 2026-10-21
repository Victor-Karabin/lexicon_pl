package com.lexicon.presentation.countries

import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.LoadCountriesUseCase
import com.lexicon.interactors.countries.LoadCountryChoiceUseCase
import com.lexicon.interactors.countries.SaveCountryChoiceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountryCourseViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val countries = listOf(
        country("BY", "Belarus", "Białoruś"),
        country("CI", "Côte d'Ivoire", "Wybrzeże Kości Słoniowej"),
        country("DE", "Germany", "Niemcy"),
        country("KN", "Saint Kitts and Nevis", "Saint Kitts i Nevis"),
        country("LC", "Saint Lucia", "Saint Lucia"),
        country("NR", "Nauru", "Naoero", "Nauru"),
        country("PL", "Poland", "Polska"),
        country("ST", "São Tomé and Príncipe", "Wyspy Świętego Tomasza i Książęca"),
    )

    private val loadCountries: LoadCountriesUseCase = mockk()
    private val loadChoice: LoadCountryChoiceUseCase = mockk()
    private val saveChoice: SaveCountryChoiceUseCase = mockk(relaxed = true)

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun country(
        code: String,
        english: String,
        vararg polish: String,
    ) = Country(code, english, polish.toList(), "man", "woman", "Stolica", "Capital")

    private fun TestScope.viewModel(chosen: Set<String> = emptySet()): CountryCourseViewModel {
        coEvery { loadCountries() } returns persistentListOf(*countries.toTypedArray())
        coEvery { loadChoice() } returns chosen
        return CountryCourseViewModel(loadCountries, loadChoice, saveChoice, dispatchers).also { advanceUntilIdle() }
    }

    private fun CountryCourseViewModel.shownCodes() = uiState.value.shown.map { it.code }

    @Test
    fun `every country is listed and the saved choice is ticked`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(chosen = setOf("DE", "PL"))

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(countries.map { it.code }, viewModel.shownCodes())
            assertEquals(setOf("DE", "PL"), viewModel.uiState.value.chosen)
        }

    @Test
    fun `nothing is ticked and Start is off when nothing was ever chosen`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            assertEquals(emptySet<String>(), viewModel.uiState.value.chosen)
            assertFalse(viewModel.uiState.value.canStart)
        }

    @Test
    fun `a search matches the English name and every Polish name without regard to case or Polish letters`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onQueryChanged("bialorus")
            assertEquals(listOf("BY"), viewModel.shownCodes())

            viewModel.onQueryChanged("nauru")
            assertEquals(listOf("NR"), viewModel.shownCodes())

            viewModel.onQueryChanged("Naoero")
            assertEquals(listOf("NR"), viewModel.shownCodes())

            viewModel.onQueryChanged("GERM")
            assertEquals(listOf("DE"), viewModel.shownCodes())

            viewModel.onQueryChanged("saint")
            assertEquals(listOf("KN", "LC"), viewModel.shownCodes())
        }

    @Test
    fun `a search also ignores accents that are not Polish`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onQueryChanged("cote")
            assertEquals(listOf("CI"), viewModel.shownCodes())

            viewModel.onQueryChanged("Côte")
            assertEquals(listOf("CI"), viewModel.shownCodes())

            viewModel.onQueryChanged("sao tome")
            assertEquals(listOf("ST"), viewModel.shownCodes())

            viewModel.onQueryChanged("principe")
            assertEquals(listOf("ST"), viewModel.shownCodes())
        }

    @Test
    fun `a search typed before the list arrives is applied to it`() =
        runTest(testDispatcher) {
            coEvery { loadCountries() } returns persistentListOf(*countries.toTypedArray())
            coEvery { loadChoice() } returns emptySet()
            val viewModel = CountryCourseViewModel(loadCountries, loadChoice, saveChoice, dispatchers)

            viewModel.onQueryChanged("germ")
            advanceUntilIdle()

            assertEquals(listOf("DE"), viewModel.shownCodes())
        }

    @Test
    fun `a search does not look at capitals`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onQueryChanged("Capital")
            assertEquals(emptyList<String>(), viewModel.shownCodes())

            viewModel.onQueryChanged("Stolica")
            assertEquals(emptyList<String>(), viewModel.shownCodes())
        }

    @Test
    fun `a blank search shows everything again`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onQueryChanged("germ")
            viewModel.onQueryChanged("  ")

            assertEquals(countries.map { it.code }, viewModel.shownCodes())
        }

    @Test
    fun `a tap ticks a country and a second tap unticks it`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onToggled("PL")
            assertEquals(setOf("PL"), viewModel.uiState.value.chosen)

            viewModel.onToggled("DE")
            viewModel.onToggled("PL")
            assertEquals(setOf("DE"), viewModel.uiState.value.chosen)
        }

    @Test
    fun `select all ticks every country when no search is typed`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onSelectAll()

            assertEquals(countries.map { it.code }.toSet(), viewModel.uiState.value.chosen)
        }

    @Test
    fun `select all with a search ticks only the rows shown and keeps the other ticks`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(chosen = setOf("PL"))

            viewModel.onQueryChanged("saint")
            viewModel.onSelectAll()

            assertEquals(setOf("PL", "KN", "LC"), viewModel.uiState.value.chosen)
        }

    @Test
    fun `clear unticks everything when no search is typed`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(chosen = setOf("PL", "DE"))

            viewModel.onClear()

            assertEquals(emptySet<String>(), viewModel.uiState.value.chosen)
            assertFalse(viewModel.uiState.value.canStart)
        }

    @Test
    fun `clear with a search unticks only the rows shown`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(chosen = setOf("PL", "KN", "LC"))

            viewModel.onQueryChanged("saint")
            viewModel.onClear()

            assertEquals(setOf("PL"), viewModel.uiState.value.chosen)
        }

    @Test
    fun `ticks made under one search survive a change of search`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.onQueryChanged("germ")
            viewModel.onToggled("DE")
            viewModel.onQueryChanged("pol")
            viewModel.onToggled("PL")
            viewModel.onQueryChanged("")

            assertEquals(setOf("DE", "PL"), viewModel.uiState.value.chosen)
        }

    @Test
    fun `Start saves exactly the ticked countries and then reports that it started`() =
        runTest(testDispatcher) {
            val viewModel = viewModel(chosen = setOf("DE"))
            viewModel.onToggled("PL")
            assertTrue(viewModel.uiState.value.canStart)

            viewModel.started.test {
                viewModel.onStart()
                advanceUntilIdle()

                awaitItem()
            }
            coVerify(exactly = 1) { saveChoice(setOf("DE", "PL")) }
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun `Start does nothing while nothing is ticked`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()

            viewModel.started.test {
                viewModel.onStart()
                advanceUntilIdle()

                expectNoEvents()
            }
            coVerify(exactly = 0) { saveChoice(any()) }
        }
}
