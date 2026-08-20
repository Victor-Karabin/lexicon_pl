package com.lexicon.data.repository

import com.lexicon.data.local.StudyDayDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyRecordRepositoryImplTest {
    private val studyDayDao: StudyDayDao = mockk()
    private val repository = StudyRecordRepositoryImpl(studyDayDao)

    private fun studied(vararg epochDays: Long) {
        coEvery { studyDayDao.studiedDaysDescending() } returns epochDays.sortedDescending()
    }

    @Test
    fun `an unbroken run ending today is the whole run`() =
        runTest {
            studied(TODAY, TODAY - 1, TODAY - 2, TODAY - 3)
            assertEquals(4, repository.currentStreak(TODAY))
        }

    @Test
    fun `a streak survives today being empty, because the day is not over`() =
        runTest {
            studied(TODAY - 1, TODAY - 2, TODAY - 3)
            assertEquals(3, repository.currentStreak(TODAY))
        }

    @Test
    fun `a streak does not survive yesterday being empty`() =
        runTest {
            studied(TODAY - 2, TODAY - 3, TODAY - 4)
            assertEquals(0, repository.currentStreak(TODAY))
        }

    @Test
    fun `only the run nearest today counts`() =
        runTest {
            studied(TODAY, TODAY - 1, TODAY - 10, TODAY - 11, TODAY - 12)
            assertEquals(2, repository.currentStreak(TODAY))
        }

    @Test
    fun `never having studied is not a streak`() =
        runTest {
            studied()
            assertEquals(0, repository.currentStreak(TODAY))
        }

    @Test
    fun `a single day counts`() =
        runTest {
            studied(TODAY)
            assertEquals(1, repository.currentStreak(TODAY))
        }

    private companion object {
        const val TODAY = 20_000L
    }
}
