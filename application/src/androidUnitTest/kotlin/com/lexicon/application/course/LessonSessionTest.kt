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
import com.lexicon.model.course.WritingReview
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    ) = LessonScreen.Reference(id, id, "", persistentListOf(), transcript, persistentListOf(), persistentListOf(), persistentListOf())

    private val writing = LessonScreen.FreeWriting(
        id = "2B",
        title = "2B",
        instruction = "",
        tracks = persistentListOf(),
        transcript = null,
        fields = persistentListOf("formal", "informal"),
        model = persistentListOf("— Dzień dobry."),
        checklist = persistentListOf(),
    )

    private val script = LessonScript(
        lessonId = LessonId("lesson"),
        steps = persistentListOf(
            LessonStep("1", "One", persistentListOf("1A", "1B")),
            LessonStep("2", "Two", persistentListOf("2A", "2B")),
        ),
        screens = persistentListOf(
            reference("1A", Transcript(TranscriptUnlock.AfterScreen("1B"), persistentListOf())),
            write("1B", "jeden", "dwa", "trzy", "cztery", "pięć", transcript = Transcript(TranscriptUnlock.AfterCheck, persistentListOf())),
            write("2A", "sześć"),
            writing,
        ),
    )

    private fun answered(
        session: LessonSession,
        vararg values: String,
    ) = values.foldIndexed(session) { index, current, value -> current.withAnswer(index.toString(), value) }

    private val atDictation = LessonSession(script).next()

    @Test
    fun `a screen with nothing to answer moves on with next`() {
        assertEquals("1B", LessonSession(script).next().screen.id)
        assertTrue("1A" in atDictation.progress.finished)
    }

    @Test
    fun `a graded screen needs a check before next, and every answer filled in before the check`() {
        assertTrue(atDictation.needsCheck)
        assertEquals(atDictation, atDictation.next())
        assertFalse(answered(atDictation, "jeden", "dwa").canCheck)
        val checked = answered(atDictation, "jeden", "dwa", "trzy", "cztery", "pięć").check()
        assertFalse(checked.needsCheck)
        assertEquals("2A", checked.next().screen.id)
    }

    @Test
    fun `answers cannot change once a screen is checked`() {
        val checked = answered(atDictation, "jeden", "dwa", "trzy", "cztery", "pięć").check()
        assertEquals("jeden", checked.withAnswer("0", "zero").answer("1B", "0"))
    }

    @Test
    fun `verdicts appear only after the check`() {
        val typed = answered(atDictation, "jeden", "dwa", "trzy", "x", "piec")
        assertNull(typed.verdict("1B", "4"))
        val checked = typed.check()
        assertEquals(AnswerVerdict.ALMOST, checked.verdict("1B", "4"))
        assertEquals(AnswerVerdict.WRONG, checked.verdict("1B", "3"))
        assertEquals(3, checked.screenScore(checked.screen).correct)
    }

    @Test
    fun `free writing is checked once something is written, and a review is kept with the answers`() {
        val atWriting = answered(answered(atDictation, "jeden", "dwa", "trzy", "cztery", "pięć").check().next(), "sześć").check().next()
        assertEquals("2B", atWriting.screen.id)
        assertFalse(atWriting.canCheck)
        val typed = atWriting.withAnswer("0", "Dzień dobry.")
        assertTrue(typed.canCheck)

        val review = WritingReview(persistentListOf("Good greeting."), persistentListOf())
        val reviewed = typed.withReview(review)
        assertEquals(review, reviewed.review("2B"))
        assertTrue(reviewed.isAtEnd)

        val unreviewed = typed.check()
        assertTrue("without a review the screen still finishes, so the lesson is never stuck", unreviewed.isAtEnd)
        assertNull(unreviewed.review("2B"))
    }

    @Test
    fun `results count graded answers per step and overall`() {
        val first = answered(atDictation, "jeden", "dwa", "trzy", "cztery", "piec").check().next()
        val second = answered(first, "siedem").check().next().withAnswer("0", "Cześć!").check()
        val results = second.results
        assertEquals(listOf(4 to 5, 0 to 1), results.steps.map { it.score.correct to it.score.total })
        assertEquals(4, results.overall.correct)
        assertEquals(6, results.overall.total)
        assertEquals(66, results.overall.percent)
    }

    @Test
    fun `a finished lesson starts over when opened again, an unfinished one resumes`() {
        val half = answered(atDictation, "jeden", "dwa", "trzy", "cztery", "pięć").check()
        assertEquals("1B", LessonSession.resume(script, half.progress).screen.id)
        assertTrue(LessonSession.isInProgress(script, half.progress))

        val done = answered(half.next(), "sześć").check().next().withAnswer("0", "Cześć!").check()
        assertTrue(done.isAtEnd)
        assertEquals("1A", LessonSession.resume(script, done.progress).screen.id)
        assertFalse(LessonSession.isInProgress(script, done.progress))
        assertFalse(LessonSession.isInProgress(script, null))
    }

    @Test
    fun `a step with exactly 80 percent right is passed and the lesson moves on`() {
        val checked = answered(atDictation, "jeden", "dwa", "trzy", "cztery", "x").check()
        assertNull(checked.failedStep)
        assertEquals("2A", checked.next().screen.id)
    }

    @Test
    fun `a step below 80 percent starts again from its first screen, keeping the right answers`() {
        val checked = answered(atDictation, "jeden", "dwa", "trzy", "x", "y").check()
        val failed = checked.failedStep
        assertEquals("1", failed?.step?.id)
        assertEquals(3 to 5, failed?.score?.let { it.correct to it.total })

        val restarted = checked.next()
        assertEquals("1A", restarted.screen.id)
        assertEquals("jeden", restarted.answer("1B", "0"))
        assertEquals("", restarted.answer("1B", "3"))
        assertEquals("", restarted.answer("1B", "4"))
        assertFalse(restarted.isChecked("1B"))
        assertTrue("1A" in restarted.progress.finished)
        assertNull(restarted.failedStep)
        assertTrue(restarted.next().needsCheck)
    }

    @Test
    fun `the failed last step starts again instead of finishing the lesson, even after a restart of the app`() {
        val passedFirst = answered(atDictation, "jeden", "dwa", "trzy", "cztery", "pięć").check().next()
        val written = answered(passedFirst, "siedem").check().next().withAnswer("0", "Cześć!").check()
        assertFalse(written.isAtEnd)
        assertEquals("2", written.failedStep?.step?.id)
        assertEquals("2B", LessonSession.resume(script, written.progress).screen.id)

        val restarted = written.next()
        assertEquals("2A", restarted.screen.id)
        assertEquals("", restarted.answer("2A", "0"))
        assertEquals("the free writing is not graded, so it is kept", "Cześć!", restarted.answer("2B", "0"))
    }

    @Test
    fun `a step that ends on a screen without answers is still held to the pass mark`() {
        val endsOnReading = LessonScript(
            lessonId = LessonId("lesson"),
            steps = persistentListOf(
                LessonStep("1", "One", persistentListOf("1A", "1B")),
                LessonStep("2", "Two", persistentListOf("2A")),
            ),
            screens = persistentListOf(write("1A", "jeden", "dwa"), reference("1B"), write("2A", "trzy")),
        )
        val failed = answered(LessonSession(endsOnReading), "x", "y").check().next()
        assertEquals("1B", failed.screen.id)
        assertEquals("1", failed.failedStep?.step?.id)
        assertEquals("1A", failed.next().screen.id)

        val passed = answered(LessonSession(endsOnReading), "jeden", "dwa").check().next()
        assertNull(passed.failedStep)
        assertEquals("2A", passed.next().screen.id)
    }

    @Test
    fun `a screen that was all right stays done when its step starts again`() {
        val twoScreens = LessonScript(
            lessonId = LessonId("lesson"),
            steps = persistentListOf(LessonStep("1", "One", persistentListOf("1A", "1B"))),
            screens = persistentListOf(write("1A", "jeden", "dwa"), write("1B", "trzy", "cztery", "pięć")),
        )
        val first = answered(LessonSession(twoScreens), "jeden", "dwa").check().next()
        val failed = answered(first, "x", "y", "pięć").check()
        assertEquals(3 to 5, failed.failedStep?.score?.let { it.correct to it.total })

        val restarted = failed.next()
        assertEquals("1A", restarted.screen.id)
        assertTrue(restarted.isChecked("1A"))
        assertFalse(restarted.needsCheck)
        assertFalse(restarted.isChecked("1B"))
        assertEquals("pięć", restarted.answer("1B", "2"))
    }

    @Test
    fun `transcripts open after their own check or after the screen they wait for`() {
        val start = LessonSession(script)
        assertFalse(start.isTranscriptUnlocked(script.screens[0]))
        assertFalse(start.isTranscriptUnlocked(script.screens[1]))
        val checked = answered(atDictation, "jeden", "dwa", "trzy", "cztery", "pięć").check()
        assertTrue(checked.isTranscriptUnlocked(script.screens[0]))
        assertTrue(checked.isTranscriptUnlocked(script.screens[1]))
    }
}
