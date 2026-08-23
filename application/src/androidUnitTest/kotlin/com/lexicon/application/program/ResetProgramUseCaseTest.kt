package com.lexicon.application.program

import com.lexicon.boundary.ProgramEnrolmentBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.interactors.program.ProgramDifficulty
import com.lexicon.interactors.program.ProgramVisibility
import com.lexicon.interactors.program.ResolveProgramScopeUseCase
import com.lexicon.model.program.ProgramId
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val NOW_MILLIS = 1_700_000_000_000
private const val STARTED_LONG_AGO = 1L

class ResetProgramUseCaseTest {
    private val id = ProgramId("mine")
    private val clock = FixedClock(NOW_MILLIS)

    private val programs: ProgramRepository = mockk()
    private val reviews: ReviewScheduleRepository = mockk()
    private val getProgram: GetProgramUseCase = mockk()
    private val resolveScope: ResolveProgramScopeUseCase = mockk()
    private val saved = slot<ProgramEnrolmentBoundary>()

    private val reset = ResetProgramUseCaseImpl(programs, getProgram, resolveScope, reviews, clock)

    @Before
    fun setUp() {
        coEvery { getProgram(id) } returns
            Program(
                id = id,
                level = "",
                order = 0,
                title = LocalizedText(mapOf("en" to "Mine")),
                description = LocalizedText(emptyMap()),
                difficulty = ProgramDifficulty.BEGINNER,
                estimatedDays = 0,
                visibility = ProgramVisibility.PRIVATE,
                config = ProgramConfig(),
            )
        coEvery { resolveScope(any()) } returns listOf(1L, 2L, 3L).map(::VocabularyId).toImmutableList()
        coEvery { programs.enrolment(id.value) } returns
            ProgramEnrolmentBoundary(
                programId = id.value,
                startedAtEpochDay = STARTED_LONG_AGO,
                status = "ACTIVE",
                completedAtEpochDay = 4,
            )
        coJustRun { programs.clearProgress(any()) }
        coJustRun { programs.saveEnrolment(capture(saved)) }
        coJustRun { reviews.forget(any()) }
    }

    @Test
    fun `every day, milestone and reward the program recorded is dropped`() =
        runTest {
            reset(id)

            coVerify { programs.clearProgress(id.value) }
        }

    @Test
    fun `the words the program taught are unscheduled so they can be learned again`() =
        runTest {
            reset(id)

            coVerify { reviews.forget(listOf(1L, 2L, 3L)) }
        }

    @Test
    fun `the enrolment starts again from today`() =
        runTest {
            reset(id)

            assertEquals(clock.todayEpochDay(), saved.captured.startedAtEpochDay)
            assertEquals(null, saved.captured.completedAtEpochDay)
        }
}
