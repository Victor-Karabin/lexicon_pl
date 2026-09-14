package com.lexicon.application.program

import com.lexicon.boundary.ProgramBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.program.DailyPlanConfig
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.model.program.ProgramId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveActiveProgramUseCaseImplTest {
    private val stored = MutableStateFlow<List<ProgramBoundary>>(emptyList())
    private val studySet = MutableStateFlow<Set<Long>>(emptySet())

    private val programs: ProgramRepository = mockk { every { observePrograms() } returns stored }
    private val vocabulary: VocabularyRepository = mockk { every { observeStudySetIds() } returns studySet }

    private val useCase = ObserveActiveProgramUseCaseImpl(programs, vocabulary)

    private fun program(
        id: String,
        queue: List<String>,
        order: Int = 0,
    ) = ProgramBoundary(
        id = id,
        level = "",
        order = order,
        title = mapOf("en" to id),
        description = emptyMap(),
        difficulty = "BEGINNER",
        estimatedDays = 0,
        visibility = "PRIVATE",
        configJson = Json.encodeToString(ProgramConfig.serializer(), ProgramConfig(dailyPlan = DailyPlanConfig(queue = queue))),
    )

    @Test
    fun `starred words and a queue mean the program is already running`() =
        runTest {
            stored.value = listOf(program("mine", queue = listOf("dictation")))
            studySet.value = setOf(1L, 2L)

            assertEquals(ProgramId("mine"), useCase().first()?.id)
        }

    @Test
    fun `nothing starred means nothing is running`() =
        runTest {
            stored.value = listOf(program("mine", queue = listOf("dictation")))

            assertNull(useCase().first())
        }

    @Test
    fun `a program with an empty queue is not running`() =
        runTest {
            stored.value = listOf(program("mine", queue = emptyList()))
            studySet.value = setOf(1L)

            assertNull(useCase().first())
        }

    @Test
    fun `the first program with a queue is the one that runs`() =
        runTest {
            stored.value = listOf(
                program("later", queue = listOf("dictation"), order = 2),
                program("empty", queue = emptyList(), order = 0),
                program("first", queue = listOf("puzzle"), order = 1),
            )
            studySet.value = setOf(1L)

            assertEquals(ProgramId("first"), useCase().first()?.id)
        }

    @Test
    fun `unstarring every word stops the program and starring one starts it again`() =
        runTest {
            stored.value = listOf(program("mine", queue = listOf("dictation")))
            studySet.value = setOf(1L)
            val seen = mutableListOf<Program?>()
            val watching = launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(seen) }

            studySet.value = emptySet()
            studySet.value = setOf(3L)
            studySet.value = setOf(3L, 4L)
            watching.cancel()

            assertEquals(listOf("mine", null, "mine"), seen.map { it?.id?.value })
        }
}
