package com.lexicon.application.course

import com.lexicon.boundary.CourseRepository
import com.lexicon.interactors.course.LessonSession
import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.LessonStep
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonPassMarkTest {
    private val screen = LessonScreen.Write(
        id = "1A",
        title = "1A",
        instruction = "",
        tracks = persistentListOf(),
        transcript = null,
        keyboard = AnswerKeyboard.TEXT,
        questions = persistentListOf(
            *listOf("jeden", "dwa", "trzy", "cztery", "pięć").mapIndexed { index, answer ->
                LessonQuestion(index.toString(), ('a' + index).toString(), persistentListOf(answer), null)
            }.toTypedArray(),
        ),
    )

    private val script = LessonScript(
        lessonId = LessonId("lesson"),
        steps = persistentListOf(LessonStep("1", "One", persistentListOf("1A"))),
        screens = persistentListOf(screen),
    )

    private val threeOfFive = listOf("jeden", "dwa", "trzy", "x", "y")
        .foldIndexed(LessonSession(script, passMark = PASS_MARK)) { index, session, value -> session.withAnswer(index.toString(), value) }
        .check()

    @Test
    fun `a lesson finished above the learner's pass mark is not in progress`() {
        assertTrue(threeOfFive.isAtEnd)
        assertFalse(LessonSession.isInProgress(script, threeOfFive.progress, passMark = PASS_MARK))
    }

    @Test
    fun `the same attempt is still in progress under a stricter pass mark`() {
        assertTrue(LessonSession.isInProgress(script, threeOfFive.progress, passMark = STRICT_PASS_MARK))
    }

    @Test
    fun `in progress and resume agree for every pass mark`() {
        listOf(PASS_MARK, STRICT_PASS_MARK).forEach { passMark ->
            val resumed = LessonSession.resume(script, threeOfFive.progress, passMark)
            assertEquals(
                "pass mark $passMark",
                LessonSession.isInProgress(script, threeOfFive.progress, passMark),
                resumed.isChecked("1A"),
            )
        }
    }

    @Test
    fun `last results of a finished attempt are offered under the learner's pass mark only`() =
        runTest {
            val repository: CourseRepository = mockk(relaxed = true) {
                coEvery { getLessonScript(any()) } returns script
                coEvery { getLastFinishedProgress(any()) } returns null
                coEvery { getLessonScriptProgress(any()) } returns threeOfFive.progress
            }
            val results = GetLastLessonResultsUseCaseImpl(repository)

            assertNotNull(results(LessonId("lesson"), PASS_MARK))
            assertNull(results(LessonId("lesson"), STRICT_PASS_MARK))
        }

    private companion object {
        const val PASS_MARK = 50
        const val STRICT_PASS_MARK = 80
    }
}
