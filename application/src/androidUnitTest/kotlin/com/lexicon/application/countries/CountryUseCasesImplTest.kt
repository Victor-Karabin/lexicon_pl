package com.lexicon.application.countries

import com.lexicon.boundary.CountryBoundary
import com.lexicon.boundary.CountryProgressBoundary
import com.lexicon.boundary.CountryRepository
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.CountryField
import com.lexicon.interactors.course.AnswerVerdict
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CountryUseCasesImplTest {
    private val catalogue = listOf(
        CountryBoundary("AG", "Antigua and Barbuda", listOf("Antigua i Barbuda"), null, null, "Saint John’s", "Saint John's"),
        CountryBoundary("BY", "Belarus", listOf("Białoruś"), "Białorusin", "Białorusinka", "Mińsk", "Minsk"),
        CountryBoundary("DE", "Germany", listOf("Niemcy"), "Niemiec", "Niemka", "Berlin", "Berlin"),
        CountryBoundary("NR", "Nauru", listOf("Naoero", "Nauru"), "Nauruańczyk", "Nauruanka", "Yaren", "Yaren"),
        CountryBoundary("PL", "Poland", listOf("Polska"), "Polak", "Polka", "Warszawa", "Warsaw"),
    )

    private class FakeCountries(
        private val catalogue: List<CountryBoundary>,
        var chosen: List<String>? = null,
    ) : CountryRepository {
        val rows = mutableMapOf<Pair<String, String>, CountryProgressBoundary>()
        val attempts = mutableListOf<Triple<String, String, Boolean>>()

        fun row(
            code: String,
            field: CountryField,
            attempted: Int,
            streak: Int,
        ) {
            rows[code to field.name] =
                CountryProgressBoundary(code, field.name, attempted, streak, attempted - streak, streak)
        }

        fun rowNamed(
            code: String,
            field: String,
            attempted: Int,
            streak: Int,
        ) {
            rows[code to field] = CountryProgressBoundary(code, field, attempted, streak, attempted - streak, streak)
        }

        fun masterAll(country: CountryBoundary) {
            val fields = if (country.man != null) CountryField.entries else listOf(CountryField.COUNTRY, CountryField.CAPITAL)
            fields.forEach { row(country.code, it, attempted = 1, streak = 1) }
        }

        fun streakOf(
            code: String,
            field: CountryField,
        ): Int = rows.getValue(code to field.name).streak

        override suspend fun countries(): List<CountryBoundary> = catalogue

        override suspend fun chosen(): List<String>? = chosen

        override suspend fun saveChosen(codes: List<String>) {
            chosen = codes
        }

        override suspend fun progress(): List<CountryProgressBoundary> = rows.values.toList()

        override suspend fun recordAttempt(
            code: String,
            field: String,
            isCorrect: Boolean,
        ) {
            attempts += Triple(code, field, isCorrect)
            val old = rows[code to field] ?: CountryProgressBoundary(code, field, 0, 0, 0, 0)
            rows[code to field] = old.copy(
                attempted = old.attempted + 1,
                correct = old.correct + if (isCorrect) 1 else 0,
                incorrect = old.incorrect + if (isCorrect) 0 else 1,
                streak = if (isCorrect) old.streak + 1 else 0,
            )
        }
    }

    private fun repository(chosen: List<String>? = null) = FakeCountries(catalogue, chosen)

    private fun country(code: String) =
        catalogue.single { it.code == code }.let {
            Country(it.code, it.english, it.polish, it.man, it.woman, it.capital, it.capitalEnglish)
        }

    private fun CountryBoundary.fieldCount() = if (man != null && woman != null) 4 else 2

    @Test
    fun `every country is loaded with all its fields in catalogue order`() =
        runTest {
            assertEquals(catalogue.map { country(it.code) }, LoadCountriesUseCaseImpl(repository())())
        }

    @Test
    fun `there is no course until countries are chosen`() =
        runTest {
            val countries = repository()
            assertNull(LoadCountryCourseUseCaseImpl(countries)())
            assertEquals(emptySet<String>(), LoadCountryChoiceUseCaseImpl(countries)())
            assertNull(NextCountryUseCaseImpl(countries)())
        }

    @Test
    fun `an emptied choice is no course either`() =
        runTest {
            val countries = repository(chosen = emptyList())
            assertNull(LoadCountryCourseUseCaseImpl(countries)())
            assertNull(NextCountryUseCaseImpl(countries)())
        }

    @Test
    fun `the saved choice is read back as a set`() =
        runTest {
            assertEquals(setOf("PL", "DE"), LoadCountryChoiceUseCaseImpl(repository(chosen = listOf("PL", "DE")))())
        }

    @Test
    fun `a stored code missing from the catalogue is not offered as ticked`() =
        runTest {
            assertEquals(setOf("PL"), LoadCountryChoiceUseCaseImpl(repository(chosen = listOf("ZZ", "PL")))())
        }

    @Test
    fun `a code that is not in the catalogue is not saved`() =
        runTest {
            val countries = repository()
            SaveCountryChoiceUseCaseImpl(countries)(setOf("PL", "ZZ"))
            assertEquals(listOf("PL"), countries.chosen)
        }

    @Test
    fun `the choice is saved in catalogue order`() =
        runTest {
            val countries = repository()
            SaveCountryChoiceUseCaseImpl(countries)(setOf("PL", "AG"))
            assertEquals(listOf("AG", "PL"), countries.chosen)
        }

    @Test
    fun `saving an empty choice clears it`() =
        runTest {
            val countries = repository(chosen = listOf("PL"))
            SaveCountryChoiceUseCaseImpl(countries)(emptySet())
            assertEquals(emptyList<String>(), countries.chosen)
            assertNull(LoadCountryCourseUseCaseImpl(countries)())
        }

    @Test
    fun `progress counts the fields of the chosen countries, mastered and tried`() =
        runTest {
            val countries = repository(chosen = listOf("PL", "AG"))
            countries.row("PL", CountryField.COUNTRY, attempted = 1, streak = 1)
            countries.row("PL", CountryField.MAN, attempted = 2, streak = 0)

            val course = LoadCountryCourseUseCaseImpl(countries)()!!
            assertEquals(listOf("AG", "PL"), course.codes)
            assertEquals(2, course.progress.items)
            assertEquals(6, course.progress.total)
            assertEquals(1, course.progress.mastered)
            assertEquals(2, course.progress.attempted)
        }

    @Test
    fun `a country that is not chosen is left out of the progress until it is chosen again`() =
        runTest {
            val countries = repository(chosen = listOf("PL"))
            catalogue.single { it.code == "DE" }.let(countries::masterAll)

            assertEquals(0, LoadCountryCourseUseCaseImpl(countries)()!!.progress.mastered)

            countries.chosen = listOf("PL", "DE")
            val progress = LoadCountryCourseUseCaseImpl(countries)()!!.progress
            assertEquals(4, progress.mastered)
            assertEquals(4, progress.attempted)
            assertEquals(8, progress.total)
        }

    @Test
    fun `a stored code missing from the catalogue is ignored`() =
        runTest {
            val countries = repository(chosen = listOf("ZZ", "PL"))
            val course = LoadCountryCourseUseCaseImpl(countries)()!!
            assertEquals(listOf("PL"), course.codes)
            assertEquals(1, course.progress.items)
            assertEquals(4, course.progress.total)
        }

    @Test
    fun `a choice of unknown codes only is no course`() =
        runTest {
            val countries = repository(chosen = listOf("ZZ"))
            assertNull(LoadCountryCourseUseCaseImpl(countries)())
            assertNull(NextCountryUseCaseImpl(countries)())
        }

    @Test
    fun `stored progress of an unknown field is ignored`() =
        runTest {
            val countries = repository(chosen = listOf("PL"))
            countries.rowNamed("PL", "FLAG", attempted = 3, streak = 3)
            countries.row("PL", CountryField.WOMAN, attempted = 1, streak = 1)

            val progress = LoadCountryCourseUseCaseImpl(countries)()!!.progress
            assertEquals(1, progress.mastered)
            assertEquals(1, progress.attempted)
        }

    @Test
    fun `the next country is one not mastered, even when it was tried fewer times`() =
        runTest {
            val countries = repository(chosen = listOf("AG", "DE"))
            catalogue.single { it.code == "AG" }.let(countries::masterAll)
            countries.row("DE", CountryField.COUNTRY, attempted = 3, streak = 0)

            assertEquals("DE", NextCountryUseCaseImpl(countries)(random = Random(1))?.code)
        }

    @Test
    fun `the next country is the one asked the fewest steps, counted on its Country field`() =
        runTest {
            val countries = repository(chosen = listOf("PL", "DE", "NR"))
            countries.masterAll(catalogue.single { it.code == "PL" })
            countries.row("DE", CountryField.COUNTRY, attempted = 2, streak = 0)
            countries.row("NR", CountryField.COUNTRY, attempted = 1, streak = 0)
            countries.row("NR", CountryField.CAPITAL, attempted = 5, streak = 0)

            val next = NextCountryUseCaseImpl(countries)
            assertEquals("NR", next(random = Random(1))?.code)
            assertEquals("DE", next(asked = setOf("NR"), random = Random(1))?.code)
        }

    @Test
    fun `a country with two fields and one with four are on a tie when each was asked once and answered wrong`() =
        runTest {
            val countries = repository(chosen = listOf("AG", "PL"))
            listOf(CountryField.COUNTRY, CountryField.CAPITAL).forEach { countries.row("AG", it, attempted = 1, streak = 0) }
            CountryField.entries.forEach { countries.row("PL", it, attempted = 1, streak = 0) }

            val picked = (0..40).map { NextCountryUseCaseImpl(countries)(random = Random(it))?.code }.toSet()

            assertEquals(setOf("AG", "PL"), picked)
        }

    @Test
    fun `countries tried equally often are picked at random`() =
        runTest {
            val countries = repository(chosen = listOf("DE", "NR"))
            val picked = (0..40).map { NextCountryUseCaseImpl(countries)(random = Random(it))?.code }.toSet()
            assertEquals(setOf("DE", "NR"), picked)
        }

    @Test
    fun `when every chosen country was asked the session starts over rather than stopping`() =
        runTest {
            val countries = repository(chosen = listOf("PL", "DE"))
            assertNotNull(NextCountryUseCaseImpl(countries)(asked = setOf("PL", "DE")))
        }

    @Test
    fun `when every field of every chosen country is mastered training goes on over all of them`() =
        runTest {
            val countries = repository(chosen = listOf("AG", "PL"))
            countries.masterAll(catalogue.single { it.code == "AG" })
            countries.masterAll(catalogue.single { it.code == "PL" })
            assertTrue(LoadCountryCourseUseCaseImpl(countries)()!!.progress.isComplete)

            val first = NextCountryUseCaseImpl(countries)(random = Random(1))
            assertNotNull(first)
            val second = NextCountryUseCaseImpl(countries)(asked = setOf(first!!.code), random = Random(1))
            assertNotNull(second)
            assertTrue(second!!.code != first.code)
        }

    @Test
    fun `the next country comes only from the chosen ones`() =
        runTest {
            val countries = repository(chosen = listOf("BY"))
            repeat(10) { assertEquals("BY", NextCountryUseCaseImpl(countries)(random = Random(it))?.code) }
        }

    @Test
    fun `every field is checked against its own answer and recorded as right`() =
        runTest {
            val countries = repository(chosen = listOf("PL"))
            val typed = mapOf(
                CountryField.COUNTRY to "polska",
                CountryField.MAN to "Polak",
                CountryField.WOMAN to "Polka",
                CountryField.CAPITAL to "Warszawa",
            )

            val verdicts = SubmitCountryAnswersUseCaseImpl(countries)(country("PL"), typed)

            assertEquals(CountryField.entries, verdicts.keys.toList())
            assertTrue(verdicts.values.all { it == AnswerVerdict.CORRECT })
            assertEquals(
                CountryField.entries.map { Triple("PL", it.name, true) },
                countries.attempts,
            )
        }

    @Test
    fun `Naoero is also accepted as Nauru`() =
        runTest {
            val submit = SubmitCountryAnswersUseCaseImpl(repository(chosen = listOf("NR")))
            val rest = mapOf(CountryField.MAN to "x", CountryField.WOMAN to "x", CountryField.CAPITAL to "x")

            assertEquals(AnswerVerdict.CORRECT, submit(country("NR"), rest + (CountryField.COUNTRY to "Nauru"))[CountryField.COUNTRY])
            assertEquals(AnswerVerdict.CORRECT, submit(country("NR"), rest + (CountryField.COUNTRY to "naoero"))[CountryField.COUNTRY])
            assertEquals(AnswerVerdict.WRONG, submit(country("NR"), rest + (CountryField.COUNTRY to "Nauruu"))[CountryField.COUNTRY])
        }

    @Test
    fun `a name wrong only in the Polish letters is almost right and is recorded as wrong`() =
        runTest {
            val countries = repository(chosen = listOf("BY"))
            val typed = mapOf(
                CountryField.COUNTRY to "Bialorus",
                CountryField.MAN to "Białorusin",
                CountryField.WOMAN to "Białorusinka",
                CountryField.CAPITAL to "Brest",
            )

            val verdicts = SubmitCountryAnswersUseCaseImpl(countries)(country("BY"), typed)

            assertEquals(AnswerVerdict.ALMOST, verdicts[CountryField.COUNTRY])
            assertEquals(AnswerVerdict.CORRECT, verdicts[CountryField.MAN])
            assertEquals(AnswerVerdict.WRONG, verdicts[CountryField.CAPITAL])
            assertEquals(
                listOf(false, true, true, false),
                countries.attempts.map { it.third },
            )
        }

    @Test
    fun `a punctuation mark of another shape is not a mistake`() =
        runTest {
            val typed = mapOf(CountryField.COUNTRY to "antigua i barbuda", CountryField.CAPITAL to "Saint John's")

            val verdicts = SubmitCountryAnswersUseCaseImpl(repository(chosen = listOf("AG")))(country("AG"), typed)

            assertEquals(AnswerVerdict.CORRECT, verdicts[CountryField.CAPITAL])
        }

    @Test
    fun `an accent that is not Polish is still required`() =
        runTest {
            val brazil = Country("BR", "Brazil", listOf("Brazylia"), "Brazylijczyk", "Brazylijka", "Brasília", "Brasilia")
            val verdicts = SubmitCountryAnswersUseCaseImpl(repository())(
                brazil,
                mapOf(
                    CountryField.COUNTRY to "Brazylia",
                    CountryField.MAN to "Brazylijczyk",
                    CountryField.WOMAN to "Brazylijka",
                    CountryField.CAPITAL to "Brasilia",
                ),
            )
            assertEquals(AnswerVerdict.WRONG, verdicts[CountryField.CAPITAL])
        }

    @Test
    fun `a country without nationalities is checked and recorded in two fields only`() =
        runTest {
            val countries = repository(chosen = listOf("AG"))
            val typed = mapOf(
                CountryField.COUNTRY to "Antigua i Barbuda",
                CountryField.MAN to "Antigua",
                CountryField.WOMAN to "Antigua",
                CountryField.CAPITAL to "Saint John’s",
            )

            val verdicts = SubmitCountryAnswersUseCaseImpl(countries)(country("AG"), typed)

            assertEquals(listOf(CountryField.COUNTRY, CountryField.CAPITAL), verdicts.keys.toList())
            assertEquals(listOf("AG" to "COUNTRY", "AG" to "CAPITAL"), countries.attempts.map { it.first to it.second })
        }

    @Test
    fun `an empty field is empty and recorded as wrong`() =
        runTest {
            val countries = repository(chosen = listOf("PL"))
            val typed = mapOf(CountryField.COUNTRY to "Polska", CountryField.MAN to "  ")

            val verdicts = SubmitCountryAnswersUseCaseImpl(countries)(country("PL"), typed)

            assertEquals(AnswerVerdict.CORRECT, verdicts[CountryField.COUNTRY])
            assertEquals(AnswerVerdict.EMPTY, verdicts[CountryField.MAN])
            assertEquals(AnswerVerdict.EMPTY, verdicts[CountryField.WOMAN])
            assertEquals(AnswerVerdict.EMPTY, verdicts[CountryField.CAPITAL])
            assertEquals(listOf(true, false, false, false), countries.attempts.map { it.third })
        }

    @Test
    fun `one right answer masters a field and a wrong one takes it back`() =
        runTest {
            val countries = repository(chosen = listOf("PL"))
            val submit = SubmitCountryAnswersUseCaseImpl(countries)
            val load = LoadCountryCourseUseCaseImpl(countries)
            val right = mapOf(
                CountryField.COUNTRY to "Polska",
                CountryField.MAN to "Polak",
                CountryField.WOMAN to "Polka",
                CountryField.CAPITAL to "Warszawa",
            )

            submit(country("PL"), right)
            assertEquals(4, load()!!.progress.mastered)
            assertTrue(load()!!.progress.isComplete)

            submit(country("PL"), right + (CountryField.CAPITAL to "Kraków"))
            assertEquals(3, load()!!.progress.mastered)
            assertEquals(0, countries.streakOf("PL", CountryField.CAPITAL))
            assertEquals(2, countries.rows.getValue("PL" to "COUNTRY").attempted)
        }

    @Test
    fun `the total of a full catalogue choice counts four fields or two`() =
        runTest {
            val countries = repository(chosen = catalogue.map { it.code })
            assertEquals(catalogue.sumOf { it.fieldCount() }, LoadCountryCourseUseCaseImpl(countries)()!!.progress.total)
        }
}
