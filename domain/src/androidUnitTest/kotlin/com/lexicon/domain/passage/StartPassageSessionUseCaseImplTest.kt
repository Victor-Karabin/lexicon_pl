package com.lexicon.domain.passage

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.interactors.passage.PassageSessionResult
import com.lexicon.interactors.passage.StartPassageSessionRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartPassageSessionUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val generator: SentenceGenerator = mockk()
    private val settings: SettingsRepository = mockk()
    private val useCase = StartPassageSessionUseCaseImpl(vocabulary, generator, StepCountResolver(settings))

    private var stepCount = 6

    private fun givenStudySet(vararg words: String) {
        coEvery { settings.getSettings() } returns
            AppSettingsBoundary(themeMode = ThemeModeBoundary.SYSTEM, stepCount = stepCount)
        val items = words.mapIndexed { index, word ->
            VocabularyItemBoundary(index + 1L, word, "x", "x", cefr = "A1")
        }
        coEvery { vocabulary.studySetWordIds() } returns items.map { it.id }
        coEvery { vocabulary.getItemsByIds(any()) } returns items
    }

    private fun answering(sentences: Map<String, String>) {
        coEvery { generator.generate(any()) } answers {
            val word = firstArg<SentenceRequestBoundary>().word
            SentenceResultBoundary.Generated(sentences.getValue(word))
        }
    }

    private suspend fun run(): PassageSessionResult = useCase(StartPassageSessionRequest(withWordBank = true))

    @Test
    fun `every sentence shown has something to fill in`() =
        runTest {
            givenStudySet("polegać", "nowy", "gdy", "jeśli", "pewnie", "okno")
            answering(
                mapOf(
                    "polegać" to "Znam Piotra od lat i zawsze mogę na niego liczyć.",
                    "nowy" to "Mam nowy telefon.",
                    "gdy" to "Gdy wrócę do domu, zadzwonię.",
                    "jeśli" to "Jeśli jutro będzie padać, zostaniemy w domu.",
                    "pewnie" to "Pewnie wrócę wcześniej.",
                    "okno" to "Otwórz proszę okno.",
                ),
            )

            val session = run() as PassageSessionResult.Ready

            assertTrue(session.passage.sentences.isNotEmpty())
            session.passage.sentences.forEach { sentence ->
                assertTrue(
                    "no gap in ${sentence.segments}",
                    sentence.segments.any { it is PassageSegment.Gap },
                )
            }
        }

    @Test
    fun `a sentence that reached for a synonym is dropped`() =
        runTest {
            givenStudySet("polegać", "nowy", "gdy", "jeśli", "pewnie", "okno")
            answering(
                mapOf(
                    "polegać" to "Znam Piotra od lat i zawsze mogę na niego liczyć.",
                    "nowy" to "Mam nowy telefon.",
                    "gdy" to "Gdy wrócę do domu, zadzwonię.",
                    "jeśli" to "Jeśli jutro będzie padać, zostaniemy w domu.",
                    "pewnie" to "Pewnie wrócę wcześniej.",
                    "okno" to "Otwórz proszę okno.",
                ),
            )

            val session = run() as PassageSessionResult.Ready

            assertTrue(
                session.passage.sentences.none { sentence ->
                    sentence.segments.filterIsInstance<PassageSegment.Text>().any { it.text.contains("Piotra") }
                },
            )
        }

    @Test
    fun `an inflected form of the word is what gets gapped`() =
        runTest {
            givenStudySet("książka")
            answering(mapOf("książka" to "Czytam ciekawą książkę wieczorem."))

            val session = run() as PassageSessionResult.Ready

            assertEquals(listOf("książkę"), session.passage.gaps.map { it.answer })
        }

    @Test
    fun `a short word has to appear whole rather than matching anything it starts`() =
        runTest {
            givenStudySet("i")
            answering(mapOf("i" to "Ile masz lat?"))

            assertTrue(run() is PassageSessionResult.Refused)
        }

    @Test
    fun `a phrase is looked for by the word carrying its meaning`() =
        runTest {
            givenStudySet("mieć na myśli")
            answering(mapOf("mieć na myśli" to "Nie wiem, co masz na myśli."))

            val session = run() as PassageSessionResult.Ready

            assertEquals(listOf("myśli"), session.passage.gaps.map { it.answer })
        }

    @Test
    fun `each gap remembers the starred word it was inflected from`() =
        runTest {
            givenStudySet("książka")
            answering(mapOf("książka" to "Czytam ciekawą książkę wieczorem."))

            val session = run() as PassageSessionResult.Ready

            // The gap holds książkę, which the vocabulary cannot be looked up by; the
            // base form is what reaches the history and the result screen.
            assertEquals(listOf("książkę"), session.passage.gaps.map { it.answer })
            assertEquals(listOf("książka"), session.passage.gaps.map { it.word })
        }

    @Test
    fun `the number of gaps follows the step count setting`() =
        runTest {
            stepCount = 3
            givenStudySet("nowy", "okno", "gdy", "lampa", "stol", "kot", "dom", "zegar")
            answering(
                mapOf(
                    "nowy" to "Mam nowy telefon.",
                    "okno" to "Otworz okno.",
                    "gdy" to "Gdy wroce, zadzwonie.",
                    "lampa" to "Kupilem lampa wczoraj.",
                    "stol" to "To jest stol.",
                    "kot" to "Widze kot na dachu.",
                    "dom" to "To moj dom.",
                    "zegar" to "Ten zegar spieszy.",
                ),
            )

            val session = run() as PassageSessionResult.Ready

            assertEquals(3, session.passage.sentences.size)
            assertEquals(3, session.passage.gaps.size)
        }

    @Test
    fun `a longer setting means a longer passage`() =
        runTest {
            stepCount = 7
            givenStudySet("nowy", "okno", "gdy", "lampa", "stol", "kot", "dom", "zegar")
            answering(
                mapOf(
                    "nowy" to "Mam nowy telefon.",
                    "okno" to "Otworz okno.",
                    "gdy" to "Gdy wroce, zadzwonie.",
                    "lampa" to "Kupilem lampa wczoraj.",
                    "stol" to "To jest stol.",
                    "kot" to "Widze kot na dachu.",
                    "dom" to "To moj dom.",
                    "zegar" to "Ten zegar spieszy.",
                ),
            )

            val session = run() as PassageSessionResult.Ready

            assertEquals(7, session.passage.sentences.size)
        }

    /** Asking for more steps than there are studySets cannot invent them. */
    @Test
    fun `the passage is capped by how many studySet there are`() =
        runTest {
            stepCount = 20
            givenStudySet("nowy", "okno")
            answering(mapOf("nowy" to "Mam nowy telefon.", "okno" to "Otworz okno."))

            val session = run() as PassageSessionResult.Ready

            assertEquals(2, session.passage.sentences.size)
        }

    @Test
    fun `the word bank offers exactly the answers`() =
        runTest {
            givenStudySet("nowy", "okno", "gdy")
            answering(
                mapOf(
                    "nowy" to "Mam nowy telefon.",
                    "okno" to "Otwórz okno.",
                    "gdy" to "Gdy wrócę, zadzwonię.",
                ),
            )

            val session = run() as PassageSessionResult.Ready

            assertEquals(session.passage.gaps.map { it.answer }.toSet(), session.bank.toSet())
        }
}
