package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonSession
import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.LessonStep
import com.lexicon.model.course.Transcript
import com.lexicon.model.course.TranscriptUnlock
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonSessionTest {
    private fun write(
        id: String,
        vararg answers: String,
        transcript: Transcript? = null,
    ) = LessonScreen.Write(
        id = id,
        title = id,
        instruction = "",
        tracks = persistentListOf(),
        transcript = transcript,
        keyboard = AnswerKeyboard.TEXT,
        questions = answers.mapIndexed { index, answer ->
            LessonQuestion(index.toString(), ('a' + index).toString(), persistentListOf(answer), null)
        }.let { persistentListOf(*it.toTypedArray()) },
    )

    private fun reference(
        id: String,
        transcript: Transcript? = null,
    ) = LessonScreen.Reference(id, id, "", persistentListOf(), transcript, persistentListOf(), persistentListOf())

    private val script = LessonScript(
        lessonId = LessonId("lesson"),
        title = "Lesson",
        passMark = 0.8,
        steps = persistentListOf(
            LessonStep("1", "One", persistentListOf("1A", "1B")),
            LessonStep("2", "Two", persistentListOf("2A")),
        ),
        screens = persistentListOf(
            reference("1A", Transcript(TranscriptUnlock.AfterScreen("1B"), persistentListOf())),
            write("1B", "jeden", "dwa", "trzy", "cztery", "pięć", transcript = Transcript(TranscriptUnlock.AfterCheck, persistentListOf())),
            write("2A", "sześć"),
        ),
        vocabulary = persistentListOf(),
    )

    private fun answered(
        session: LessonSession,
        vararg values: String,
    ) = values.foldIndexed(session) { index, current, value -> current.withAnswer(index.toString(), value) }

    @Test
    fun `an ungraded screen moves on only once it is done`() {
        val start = LessonSession(script)
        assertEquals(start, start.next())
        assertEquals("1B", start.finish().next().screen.id)
    }

    @Test
    fun `a graded screen can be checked only when every answer is filled in`() {
        val atDictation = LessonSession(script).finish().next()
        assertFalse(answered(atDictation, "jeden", "dwa").canCheck)
        assertTrue(answered(atDictation, "jeden", "dwa", "trzy", "cztery", "pięć").canCheck)
    }

    @Test
    fun `answers cannot change once a screen is checked`() {
        val checked = answered(LessonSession(script).finish().next(), "jeden", "dwa", "trzy", "cztery", "pięć").check()
        assertEquals("jeden", checked.withAnswer("0", "zero").answer("1B", "0"))
    }

    @Test
    fun `the step score counts correct answers against the pass mark`() {
        val passed = answered(LessonSession(script).finish().next(), "jeden", "dwa", "trzy", "cztery", "piec").check()
        val score = passed.stepScore(script.steps[0])
        assertEquals(4, score.correct)
        assertEquals(5, score.total)
        assertTrue("4 of 5 is 80 %, the pass mark", score.passed)

        val failed = answered(LessonSession(script).finish().next(), "jeden", "dwa", "trzy", "x", "y").check()
        assertFalse(failed.stepScore(script.steps[0]).passed)
        assertEquals(AnswerVerdict.ALMOST, passed.verdict("1B", "4"))
    }

    @Test
    fun `retrying a step clears its answers and goes back to its first screen`() {
        val failed = answered(LessonSession(script).finish().next(), "jeden", "dwa", "trzy", "x", "y").check()
        val retried = failed.retryStep(script.steps[0])
        assertEquals("1A", retried.screen.id)
        assertFalse(retried.isChecked("1B"))
        assertEquals("", retried.answer("1B", "0"))
        assertTrue("an ungraded screen stays done", "1A" in retried.progress.finished)
    }

    @Test
    fun `transcripts open after their own check or after the screen they wait for`() {
        val start = LessonSession(script)
        assertFalse(start.isTranscriptUnlocked(script.screens[0]))
        assertFalse(start.isTranscriptUnlocked(script.screens[1]))
        val checked = answered(start.finish().next(), "jeden", "dwa", "trzy", "cztery", "pięć").check()
        assertTrue(checked.isTranscriptUnlocked(script.screens[0]))
        assertTrue(checked.isTranscriptUnlocked(script.screens[1]))
    }

    @Test
    fun `the lesson is complete only on the last screen with every step passed`() {
        val first = answered(LessonSession(script).finish().next(), "jeden", "dwa", "trzy", "cztery", "pięć").check()
        val last = first.next().withAnswer("0", "sześć").check()
        assertTrue(last.isLessonComplete)
        assertFalse(first.isLessonComplete)
    }

    @Test
    fun `a failed step can be retried from its own end and from the last screen`() {
        val failed = answered(LessonSession(script).finish().next(), "jeden", "dwa", "trzy", "x", "y").check()
        assertEquals(script.steps[0], failed.stepToRetry)

        val last = failed.next().withAnswer("0", "sześć").check()
        assertEquals("the last step passed, so the earlier failed step is offered", script.steps[0], last.stepToRetry)
        assertEquals("1A", last.retryStep(script.steps[0]).screen.id)

        val passed = answered(LessonSession(script).finish().next(), "jeden", "dwa", "trzy", "cztery", "pięć").check()
        assertEquals(null, passed.stepToRetry)
        assertEquals(null, passed.next().withAnswer("0", "sześć").check().stepToRetry)
    }

    @Test
    fun `the last screen of a step is known so the score can be shown there`() {
        val atDictation = LessonSession(script).finish().next()
        assertTrue(atDictation.endsStep)
        assertFalse(LessonSession(script).endsStep)
    }
}
