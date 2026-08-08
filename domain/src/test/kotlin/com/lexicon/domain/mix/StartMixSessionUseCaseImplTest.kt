package com.lexicon.domain.mix

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.dictation.DictationSessionResponse
import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleSessionResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.interactors.imagetest.ImageTestSessionResponse
import com.lexicon.interactors.imagetest.ImageTestStepResponse
import com.lexicon.interactors.imagetest.StartImageTestSessionUseCase
import com.lexicon.interactors.mix.MixTrainingType
import com.lexicon.interactors.mix.StartMixSessionRequest
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.puzzle.PuzzleSessionResponse
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.TrueOrFalseSessionResponse
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartMixSessionUseCaseImplTest {
    private var nextId = 0L

    private fun id() = ++nextId

    private val startDictation: StartDictationSessionUseCase = mockk {
        coEvery { this@mockk(any()) } answers {
            DictationSessionResponse("d", listOf(DictationStepResponse(0, id(), "kot", "cat")))
        }
    }
    private val startDictationPuzzle: StartDictationPuzzleSessionUseCase = mockk {
        coEvery { this@mockk(any()) } answers {
            DictationPuzzleSessionResponse("dp", listOf(DictationPuzzleStepResponse(0, id(), "pies", "dog")))
        }
    }
    private val startPuzzle: StartPuzzleSessionUseCase = mockk {
        coEvery { this@mockk(any()) } answers {
            PuzzleSessionResponse("p", listOf(PuzzleStepResponse(0, id(), "dom", null, "house")))
        }
    }
    private val startImageTest: StartImageTestSessionUseCase = mockk {
        coEvery { this@mockk(any()) } answers {
            ImageTestSessionResponse("i", listOf(ImageTestStepResponse(0, id(), null, "woda", listOf("water"), "water")))
        }
    }
    private val startTrueOrFalse: StartTrueOrFalseSessionUseCase = mockk {
        coEvery { this@mockk(any()) } answers {
            TrueOrFalseSessionResponse("t", listOf(TrueOrFalseStepResponse(0, id(), "chleb", "bread", true)))
        }
    }
    private val startPronunciation: StartPronunciationSessionUseCase = mockk {
        coEvery { this@mockk(any()) } answers {
            PronunciationSessionResponse("pr", listOf(PronunciationStepResponse(0, id(), "mleko", "milk", "ˈmlɛkɔ")))
        }
    }
    private val settingsRepository: SettingsRepository = mockk {
        coEvery { getSettings() } returns AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 10)
    }

    private val useCase = StartMixSessionUseCaseImpl(
        startDictation,
        startDictationPuzzle,
        startPuzzle,
        startImageTest,
        startTrueOrFalse,
        startPronunciation,
        StepCountResolver(settingsRepository),
    )

    @Test
    fun `Word Match, Memory Cards and Crossword are not Mix training types at all`() {
        val names = MixTrainingType.entries.map { it.name }
        assertTrue(names.none { it.contains("WORD_MATCH") })
        assertTrue(names.none { it.contains("MEMORY") })
        assertTrue(names.none { it.contains("CROSSWORD") })
        assertEquals(6, MixTrainingType.entries.size)
    }

    @Test
    fun `the session has one step per configured step count`() =
        runTest {
            assertEquals(10, useCase(StartMixSessionRequest()).steps.size)
        }

    @Test
    fun `steps are numbered consecutively from zero`() =
        runTest {
            val steps = useCase(StartMixSessionRequest(stepCount = 6)).steps
            assertEquals((0 until 6).toList(), steps.map { it.stepIndex })
        }

    @Test
    fun `every enabled training type appears when there are enough steps`() =
        runTest {
            val steps = useCase(StartMixSessionRequest(stepCount = MixTrainingType.entries.size)).steps
            assertEquals(MixTrainingType.entries.toSet(), steps.map { it.trainingType }.toSet())
        }

    @Test
    fun `only the requested training types are generated`() =
        runTest {
            val allowed = setOf(MixTrainingType.DICTATION, MixTrainingType.IMAGE_TEST)

            val steps = useCase(StartMixSessionRequest(stepCount = 8, trainingTypes = allowed)).steps

            assertTrue(steps.map { it.trainingType }.all { it in allowed })
        }

    @Test
    fun `True or False steps are requested one at a time, so no timed pool is built`() =
        runTest {
            val requests = mutableListOf<StartTrueOrFalseSessionRequest>()

            useCase(StartMixSessionRequest(stepCount = 4, trainingTypes = setOf(MixTrainingType.TRUE_OR_FALSE)))

            coVerify { startTrueOrFalse(capture(requests)) }
            assertEquals(4, requests.size)
            assertTrue("every step must be its own single question", requests.all { it.poolSize == 1 })
        }

    @Test
    fun `a training type that cannot produce a step is dropped rather than emitting an empty step`() =
        runTest {
            coEvery { startDictation(any()) } returns DictationSessionResponse("d", emptyList())

            val steps = useCase(StartMixSessionRequest(stepCount = 4, trainingTypes = setOf(MixTrainingType.DICTATION))).steps

            assertTrue(steps.isEmpty())
        }

    @Test
    fun `the same word is never asked twice under the same training type`() =
        runTest {
            val pool = listOf(1L, 2L, 3L)
            var draw = 0
            coEvery { startDictation(any()) } answers {
                DictationSessionResponse("d", listOf(DictationStepResponse(0, pool[draw++ % pool.size], "kot", "cat")))
            }

            val steps = useCase(StartMixSessionRequest(stepCount = 30, trainingTypes = setOf(MixTrainingType.DICTATION))).steps

            val exercises = steps.map { it.trainingType to it.vocabularyItemId }
            assertEquals("an exercise was repeated within the session", exercises.distinct(), exercises)
        }

    @Test
    fun `the same word may still be asked under a different training type`() =
        runTest {
            coEvery { startDictation(any()) } returns
                DictationSessionResponse("d", listOf(DictationStepResponse(0, 99L, "kot", "cat")))
            coEvery { startPuzzle(any()) } returns
                PuzzleSessionResponse("p", listOf(PuzzleStepResponse(0, 99L, "kot", null, "cat")))

            val types = setOf(MixTrainingType.DICTATION, MixTrainingType.PUZZLE)
            val steps = useCase(StartMixSessionRequest(stepCount = 2, trainingTypes = types)).steps

            assertEquals(2, steps.size)
            assertEquals(types, steps.map { it.trainingType }.toSet())
        }

    @Test
    fun `a training stuck on one word contributes a single step`() =
        runTest {
            coEvery { startDictation(any()) } returns
                DictationSessionResponse("d", listOf(DictationStepResponse(0, 7L, "kot", "cat")))

            val steps = useCase(StartMixSessionRequest(stepCount = 5, trainingTypes = setOf(MixTrainingType.DICTATION))).steps

            assertEquals(1, steps.size)
        }
}
