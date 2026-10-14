package com.lexicon.data.repository

import com.lexicon.data.local.NumberCourseEntity
import com.lexicon.data.local.NumberDao
import com.lexicon.data.local.NumberProgressEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberRepositoryImplTest {
    private val dao: NumberDao = mockk(relaxed = true)
    private val repository = NumberRepositoryImpl(dao)

    @Test
    fun `the chosen numbers are kept as typed`() =
        runTest {
            repository.saveSpec("1-20, 35")
            coVerify { dao.saveCourse(NumberCourseEntity(spec = "1-20, 35")) }
        }

    @Test
    fun `a right answer extends the streak and a wrong one resets it`() =
        runTest {
            coEvery { dao.progressFor(7, "CARDINAL", "") } returns
                NumberProgressEntity(7, "CARDINAL", "", attempted = 3, correct = 2, incorrect = 1, streak = 2)

            repository.recordAttempt(7, kind = "CARDINAL", gender = "", isCorrect = true)
            coVerify { dao.save(NumberProgressEntity(7, "CARDINAL", "", attempted = 4, correct = 3, incorrect = 1, streak = 3)) }

            repository.recordAttempt(7, kind = "CARDINAL", gender = "", isCorrect = false)
            coVerify { dao.save(NumberProgressEntity(7, "CARDINAL", "", attempted = 4, correct = 2, incorrect = 2, streak = 0)) }
        }

    @Test
    fun `a first attempt starts the counts`() =
        runTest {
            coEvery { dao.progressFor(9, "ORDINAL", "FEMININE") } returns null
            repository.recordAttempt(9, kind = "ORDINAL", gender = "FEMININE", isCorrect = true)
            coVerify { dao.save(NumberProgressEntity(9, "ORDINAL", "FEMININE", attempted = 1, correct = 1, incorrect = 0, streak = 1)) }
        }

    @Test
    fun `no numbers are chosen until they are saved`() =
        runTest {
            coEvery { dao.course() } returns null
            assertEquals(null, repository.spec())
        }
}
