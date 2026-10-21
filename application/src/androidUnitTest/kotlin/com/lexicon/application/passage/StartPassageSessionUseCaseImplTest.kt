package com.lexicon.application.passage

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.FakeSessionStore
import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.interactors.passage.PassageSessionResult
import com.lexicon.interactors.passage.StartPassageSessionRequest
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartPassageSessionUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val generator: SentenceGenerator = mockk()
    private val settings: SettingsRepository = mockk()
    private val useCase = StartPassageSessionUseCaseImpl(vocabulary, generator, StepCountResolver(settings), FakeSessionStore())

    private var stepCount = 6

    private val requested = mutableListOf<String>()

    private fun items(vararg words: String) =
        words.mapIndexed { index, word ->
            Word(VocabularyId(index + 1L), word, "x", "x", cefr = CefrLevel.A1)
        }

    private fun givenStepCount() {
        coEvery { settings.getSettings() } returns
            AppSettingsBoundary(themeMode = ThemeModeBoundary.SYSTEM, stepCount = stepCount)
    }

    private fun givenStudySet(vararg words: String) {
        givenStepCount()
        val items = items(*words)
        coEvery { vocabulary.studySetWordIds() } returns items.map { it.id.value }
        coEvery { vocabulary.getItemsByIds(any()) } returns items
    }

    private fun answering(sentences: Map<String, String>) {
        coEvery { generator.generate(any()) } answers {
            val word = firstArg<SentenceRequestBoundary>().word
            requested += word
            SentenceResultBoundary.Generated(sentences.getValue(word))
        }
    }

    private suspend fun run(vocabularyIds: List<Long> = emptyList()): PassageSessionResult =
        useCase(StartPassageSessionRequest(withWordBank = true, vocabularyIds = vocabularyIds))

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

    @Test
    fun `one refused sentence does not cost the whole passage`() =
        runTest {
            givenStudySet("nowy", "okno", "dom")
            coEvery { generator.generate(any()) } answers {
                when (val word = firstArg<SentenceRequestBoundary>().word) {
                    "dom" -> SentenceResultBoundary.Refused("busy")
                    "nowy" -> SentenceResultBoundary.Generated("Mam nowy telefon.")
                    else -> SentenceResultBoundary.Generated("Otwórz proszę $word.")
                }
            }

            val session = run() as PassageSessionResult.Ready

            assertEquals(listOf("nowy", "okno"), session.passage.gaps.map { it.word }.sorted())
        }

    @Test
    fun `a lesson's word list is the only thing sentences are written for and the study set is not read`() =
        runTest {
            givenStepCount()
            val lesson = listOf(2L, 3L)
            coEvery { vocabulary.getItemsByIds(lesson) } returns items("nowy", "okno", "gdy").filter { it.id.value in lesson }
            answering(mapOf("okno" to "Otwórz okno.", "gdy" to "Gdy wrócę, zadzwonię."))

            val session = run(lesson) as PassageSessionResult.Ready

            assertEquals(setOf("okno", "gdy"), requested.toSet())
            assertEquals(listOf("gdy", "okno"), session.passage.gaps.map { it.word }.sorted())
            coVerify(exactly = 0) { vocabulary.studySetWordIds() }
        }

    @Test
    fun `without a word list the study set is used`() =
        runTest {
            givenStudySet("nowy", "okno")
            answering(mapOf("nowy" to "Mam nowy telefon.", "okno" to "Otwórz okno."))

            run(vocabularyIds = emptyList())

            assertEquals(setOf("nowy", "okno"), requested.toSet())
            coVerify(exactly = 1) { vocabulary.studySetWordIds() }
        }

    @Test
    fun `a lesson with fewer words than the step count gives fewer sentences and no top-up`() =
        runTest {
            stepCount = 6
            givenStepCount()
            val lesson = listOf(1L, 2L)
            coEvery { vocabulary.getItemsByIds(lesson) } returns items("nowy", "okno")
            answering(mapOf("nowy" to "Mam nowy telefon.", "okno" to "Otwórz okno."))

            val session = run(lesson) as PassageSessionResult.Ready

            assertEquals(2, session.passage.sentences.size)
            assertEquals(setOf("nowy", "okno"), requested.toSet())
        }

    @Test
    fun `a lesson word whose sentence is refused leaves one sentence fewer`() =
        runTest {
            stepCount = 6
            givenStepCount()
            val lesson = listOf(1L, 2L, 3L)
            coEvery { vocabulary.getItemsByIds(lesson) } returns items("nowy", "okno", "dom")
            coEvery { generator.generate(any()) } answers {
                when (val word = firstArg<SentenceRequestBoundary>().word) {
                    "dom" -> SentenceResultBoundary.Refused("busy")
                    "nowy" -> SentenceResultBoundary.Generated("Mam nowy telefon.")
                    else -> SentenceResultBoundary.Generated("Otwórz proszę $word.")
                }
            }

            val session = run(lesson) as PassageSessionResult.Ready

            assertEquals(2, session.passage.sentences.size)
        }

    private fun givenWords(vararg words: Word) {
        givenStepCount()
        coEvery { vocabulary.studySetWordIds() } returns words.map { it.id.value }
        coEvery { vocabulary.getItemsByIds(any()) } returns words.toList()
    }

    private fun capturingRequests(): List<SentenceRequestBoundary> {
        val requests = mutableListOf<SentenceRequestBoundary>()
        coEvery { generator.generate(capture(requests)) } answers {
            SentenceResultBoundary.Generated("Mam ${firstArg<SentenceRequestBoundary>().word}.")
        }
        return requests
    }

    @Test
    fun `when every sentence is offline the passage is offline`() =
        runTest {
            givenStudySet("nowy", "okno")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Offline

            assertEquals(PassageSessionResult.Offline, run())
        }

    @Test
    fun `offline wins over a refusal`() =
        runTest {
            givenStudySet("nowy", "okno")
            coEvery { generator.generate(any()) } answers {
                if (firstArg<SentenceRequestBoundary>().word == "nowy") {
                    SentenceResultBoundary.Offline
                } else {
                    SentenceResultBoundary.Refused("busy")
                }
            }

            assertEquals(PassageSessionResult.Offline, run())
        }

    @Test
    fun `a refusal alone passes its reason on`() =
        runTest {
            givenStudySet("nowy")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Refused("busy")

            assertEquals(PassageSessionResult.Refused("busy"), run())
        }

    @Test
    fun `a refusal reason is passed on even when the other sentences never used their word`() =
        runTest {
            givenStudySet("nowy", "okno")
            coEvery { generator.generate(any()) } answers {
                if (firstArg<SentenceRequestBoundary>().word == "nowy") {
                    SentenceResultBoundary.Refused("busy")
                } else {
                    SentenceResultBoundary.Generated("Zupełnie inne zdanie.")
                }
            }

            assertEquals(PassageSessionResult.Refused("busy"), run())
        }

    @Test
    fun `sentences that never use their word are refused with a fixed reason`() =
        runTest {
            givenStudySet("nowy", "okno")
            answering(mapOf("nowy" to "Zupełnie inne zdanie.", "okno" to "Kolejne inne zdanie."))

            assertEquals(PassageSessionResult.Refused("no sentence used the word it was given"), run())
        }

    @Test
    fun `sentences are asked for with no context and no required words`() =
        runTest {
            givenWords(Word(VocabularyId(1), "nowy", "new", "x", cefr = CefrLevel.A2))
            val requests = capturingRequests()

            run()

            assertEquals(
                listOf(
                    SentenceRequestBoundary(
                        word = "nowy",
                        translation = "new",
                        level = "A2",
                        context = "",
                        requiredWords = emptyList(),
                    ),
                ),
                requests,
            )
        }

    @Test
    fun `the level is the highest level among the words asked about`() =
        runTest {
            givenWords(
                Word(VocabularyId(1), "nowy", "x", "x", cefr = CefrLevel.A2),
                Word(VocabularyId(2), "okno", "x", "x", cefr = CefrLevel.B1),
                Word(VocabularyId(3), "dom", "x", "x"),
            )
            val requests = capturingRequests()

            run()

            assertEquals(3, requests.size)
            assertTrue(requests.all { it.level == "B1" })
        }

    @Test
    fun `without any level among the words the level is A1`() =
        runTest {
            givenWords(Word(VocabularyId(1), "nowy", "x", "x"), Word(VocabularyId(2), "okno", "x", "x"))
            val requests = capturingRequests()

            run()

            assertTrue(requests.all { it.level == "A1" })
        }

    @Test
    fun `two spare sentences are asked for on top of the step count`() =
        runTest {
            stepCount = 3
            givenStudySet("nowy", "okno", "gdy", "lampa", "stol", "kot", "dom", "zegar")
            val requests = capturingRequests()

            run()

            assertEquals(5, requests.size)
            assertEquals(5, requests.map { it.word }.toSet().size)
        }
}
