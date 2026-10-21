package com.lexicon.presentation.countries

import app.cash.turbine.test
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.CountryField
import com.lexicon.interactors.countries.NextCountryUseCase
import com.lexicon.interactors.countries.SubmitCountryAnswersUseCase
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TextResource
import com.lexicon.presentation.common.WordResultEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableMap
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountryTrainingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val poland = Country("PL", "Poland", listOf("Polska"), "Polak", "Polka", "Warszawa", "Warsaw")
    private val antigua = Country("AG", "Antigua and Barbuda", listOf("Antigua i Barbuda"), null, null, "Saint John’s", "Saint John's")
    private val belarus = Country("BY", "Belarus", listOf("Białoruś"), "Białorusin", "Białorusinka", "Mińsk", "Minsk")

    private val nextCountry: NextCountryUseCase = mockk()
    private val submitAnswers: SubmitCountryAnswersUseCase = mockk()
    private val speechSynthesizer: SpeechSynthesizer = mockk(relaxed = true)
    private val results = LastSessionResultsHolder()

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): CountryTrainingViewModel =
        CountryTrainingViewModel(nextCountry, submitAnswers, speechSynthesizer, results, mockk(relaxed = true), dispatchers)
            .also { advanceUntilIdle() }

    private fun TestScope.fill(
        viewModel: CountryTrainingViewModel,
        country: Country,
        typed: (CountryField) -> String = { country.answers(it).first() },
    ) {
        country.fields.forEach { viewModel.onAnswerChanged(it, typed(it)) }
        advanceUntilIdle()
    }

    private fun verdicts(
        country: Country,
        verdict: AnswerVerdict = AnswerVerdict.CORRECT,
    ) = country.fields.associateWith { verdict }.toImmutableMap()

    @Test
    fun `with no country to practise the screen stops loading and shows none`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returns null

            val viewModel = viewModel()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.country)
        }

    @Test
    fun `the first country is shown with the session length`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returns poland

            val viewModel = viewModel()

            assertEquals(poland, viewModel.uiState.value.country)
            assertEquals(0, viewModel.uiState.value.stepIndex)
            assertEquals(AppSettings.DEFAULT_STEP_COUNT, viewModel.uiState.value.totalSteps)
        }

    @Test
    fun `Check needs every field of the country filled, four for most and two for the rest`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returnsMany listOf(poland, antigua)
            coEvery { submitAnswers(any(), any()) } returns verdicts(poland)

            val viewModel = viewModel()
            assertFalse(viewModel.uiState.value.canCheck)

            viewModel.onAnswerChanged(CountryField.COUNTRY, "Polska")
            viewModel.onAnswerChanged(CountryField.MAN, "Polak")
            viewModel.onAnswerChanged(CountryField.WOMAN, "Polka")
            viewModel.onAnswerChanged(CountryField.CAPITAL, "   ")
            assertFalse(viewModel.uiState.value.canCheck)

            viewModel.onAnswerChanged(CountryField.CAPITAL, "Warszawa")
            assertTrue(viewModel.uiState.value.canCheck)

            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(antigua, viewModel.uiState.value.country)
            viewModel.onAnswerChanged(CountryField.COUNTRY, "Antigua i Barbuda")
            assertFalse(viewModel.uiState.value.canCheck)
            viewModel.onAnswerChanged(CountryField.CAPITAL, "Saint John’s")
            assertTrue(viewModel.uiState.value.canCheck)
        }

    @Test
    fun `Check does nothing while a field is empty`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returns poland

            val viewModel = viewModel()
            viewModel.onAnswerChanged(CountryField.COUNTRY, "Polska")
            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 0) { submitAnswers(any(), any()) }
            assertTrue(viewModel.uiState.value.verdicts.isEmpty())
        }

    @Test
    fun `Check hands over what was typed and keeps the verdicts`() =
        runTest(testDispatcher) {
            val checked = verdicts(poland).toMutableMap().apply { put(CountryField.WOMAN, AnswerVerdict.WRONG) }.toImmutableMap()
            coEvery { nextCountry(any(), any()) } returns poland
            coEvery { submitAnswers(any(), any()) } returns checked

            val viewModel = viewModel()
            fill(viewModel, poland) { if (it == CountryField.WOMAN) "polke" else poland.answers(it).first() }
            viewModel.onCheck()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                submitAnswers(
                    poland,
                    mapOf(
                        CountryField.COUNTRY to "Polska",
                        CountryField.MAN to "Polak",
                        CountryField.WOMAN to "polke",
                        CountryField.CAPITAL to "Warszawa",
                    ),
                )
            }
            assertEquals(checked, viewModel.uiState.value.verdicts)
            assertFalse(viewModel.uiState.value.isChecking)
        }

    @Test
    fun `answers cannot be edited or checked again after the check`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returns poland
            coEvery { submitAnswers(any(), any()) } returns verdicts(poland)

            val viewModel = viewModel()
            fill(viewModel, poland)
            viewModel.onCheck()
            advanceUntilIdle()
            val answers = viewModel.uiState.value.answers

            viewModel.onAnswerChanged(CountryField.COUNTRY, "Niemcy")
            viewModel.onCheck()
            advanceUntilIdle()

            assertEquals(answers, viewModel.uiState.value.answers)
            assertFalse(viewModel.uiState.value.canCheck)
            coVerify(exactly = 1) { submitAnswers(any(), any()) }
        }

    @Test
    fun `answers cannot be edited while the check is running`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returns poland
            coEvery { submitAnswers(any(), any()) } returns verdicts(poland)

            val viewModel = viewModel()
            fill(viewModel, poland)
            viewModel.onCheck()
            viewModel.onAnswerChanged(CountryField.COUNTRY, "Niemcy")
            advanceUntilIdle()

            assertEquals("Polska", viewModel.uiState.value.answers[CountryField.COUNTRY])
        }

    @Test
    fun `a second Next while the first is still moving does not skip a country`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returnsMany listOf(poland, belarus, antigua)
            coEvery { submitAnswers(any(), any()) } returns verdicts(poland)

            val viewModel = viewModel()
            fill(viewModel, poland)
            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.onNext()
            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(belarus, viewModel.uiState.value.country)
            assertEquals(1, viewModel.uiState.value.stepIndex)
            coVerify(exactly = 2) { nextCountry(any(), any()) }
        }

    @Test
    fun `the next step shows the next country with empty fields`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returnsMany listOf(poland, belarus)
            coEvery { submitAnswers(any(), any()) } returns verdicts(poland)

            val viewModel = viewModel()
            fill(viewModel, poland)
            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(belarus, viewModel.uiState.value.country)
            assertEquals(1, viewModel.uiState.value.stepIndex)
            assertTrue(viewModel.uiState.value.answers.isEmpty())
            assertTrue(viewModel.uiState.value.verdicts.isEmpty())
        }

    @Test
    fun `at the end each field is listed with the closest answer, what was typed and how it went`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returnsMany listOf(belarus, null)
            coEvery { submitAnswers(any(), any()) } returns
                mapOf(
                    CountryField.COUNTRY to AnswerVerdict.ALMOST,
                    CountryField.MAN to AnswerVerdict.CORRECT,
                    CountryField.WOMAN to AnswerVerdict.WRONG,
                    CountryField.CAPITAL to AnswerVerdict.WRONG,
                ).toImmutableMap()

            val viewModel = viewModel()
            val typed = mapOf(
                CountryField.COUNTRY to "Bialorus",
                CountryField.MAN to "Białorusin",
                CountryField.WOMAN to "Bialorusinka",
                CountryField.CAPITAL to "Brest",
            )
            fill(viewModel, belarus) { typed.getValue(it) }
            viewModel.onCheck()
            advanceUntilIdle()

            viewModel.navigationEvents.test {
                viewModel.onNext()
                advanceUntilIdle()

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 1, incorrect = 3, skipped = 0), awaitItem())
            }

            fun entry(
                field: CountryField,
                word: String,
                outcome: AnswerState,
            ) = WordResultEntry(
                word = word,
                translation = "Belarus",
                outcome = outcome,
                typed = typed.getValue(field),
                translationText = TextResource(R.string.dot_separated_pair, listOf("Belarus", TextResource(field.labelId()))),
            )
            assertEquals(
                listOf(
                    entry(CountryField.COUNTRY, "Białoruś", AnswerState.Incorrect("Białoruś")),
                    entry(CountryField.MAN, "Białorusin", AnswerState.Correct),
                    entry(CountryField.WOMAN, "Białorusinka", AnswerState.Incorrect("Białorusinka")),
                    entry(CountryField.CAPITAL, "Mińsk", AnswerState.Incorrect("Mińsk")),
                ),
                results.wordResults,
            )
        }

    @Test
    fun `a country without nationalities adds two entries to the results`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returnsMany listOf(antigua, null)
            coEvery { submitAnswers(any(), any()) } returns verdicts(antigua)

            val viewModel = viewModel()
            fill(viewModel, antigua)
            viewModel.onCheck()
            advanceUntilIdle()
            viewModel.navigationEvents.test {
                viewModel.onNext()
                advanceUntilIdle()
                assertEquals(SessionNavigationEvent.SessionComplete(correct = 2, incorrect = 0, skipped = 0), awaitItem())
            }

            assertEquals(2, results.wordResults.size)
        }

    @Test
    fun `a session is the default number of steps and a country is never asked twice in it`() =
        runTest(testDispatcher) {
            val pool = (0 until AppSettings.DEFAULT_STEP_COUNT + 2).map {
                Country("A${'A' + it}", "Country $it", listOf("Kraj $it"), "man $it", "woman $it", "Stolica $it", "Capital $it")
            }
            val askedWhenAsking = mutableListOf<Set<String>>()
            coEvery { nextCountry(any(), any()) } coAnswers {
                val asked = firstArg<Set<String>>()
                askedWhenAsking += asked.toSet()
                pool.first { it.code !in asked }
            }
            coEvery { submitAnswers(any(), any()) } coAnswers { verdicts(firstArg()) }

            val viewModel = viewModel()
            viewModel.navigationEvents.test {
                repeat(AppSettings.DEFAULT_STEP_COUNT) {
                    fill(viewModel, viewModel.uiState.value.country!!)
                    viewModel.onCheck()
                    advanceUntilIdle()
                    viewModel.onNext()
                    advanceUntilIdle()
                }

                val done = awaitItem() as SessionNavigationEvent.SessionComplete
                assertEquals(AppSettings.DEFAULT_STEP_COUNT * 4, done.correct)
                assertEquals(0, done.incorrect)
            }

            assertEquals(AppSettings.DEFAULT_STEP_COUNT, askedWhenAsking.size)
            askedWhenAsking.forEachIndexed { step, asked -> assertEquals(pool.take(step).map { it.code }.toSet(), asked) }
            assertEquals(AppSettings.DEFAULT_STEP_COUNT * 4, results.wordResults.size)
        }

    @Test
    fun `speaking an answer reads the form aloud`() =
        runTest(testDispatcher) {
            coEvery { nextCountry(any(), any()) } returns poland

            val viewModel = viewModel()
            viewModel.onSpeak("Warszawa")
            advanceUntilIdle()

            coVerify(exactly = 1) { speechSynthesizer.speak("Warszawa") }
        }
}
