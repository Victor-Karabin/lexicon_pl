package com.lexicon.model.training

import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    private fun session(steps: Int = 3) =
        Session(
            id = SessionId("s1"),
            training = TrainingType.DICTATION,
            steps = (0 until steps).map {
                Step(index = it, wordId = VocabularyId(it + 1L), expectedAnswer = "word$it")
            }.toImmutableList(),
        )

    @Test
    fun `a session cannot exist without steps`() {
        val failure = runCatching {
            Session(SessionId("s1"), TrainingType.DICTATION, emptyList<Step>().toImmutableList())
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `a session's steps must be numbered from zero in order`() {
        val failure = runCatching {
            Session(
                SessionId("s1"),
                TrainingType.DICTATION,
                listOf(Step(1, VocabularyId(1), "a"), Step(0, VocabularyId(2), "b")).toImmutableList(),
            )
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `the current step is the first one still unanswered`() {
        val started = session()
        assertEquals(0, started.currentStep?.index)
        assertEquals(1, started.answer(0, StepOutcome.CORRECT).currentStep?.index)
    }

    @Test
    fun `a step cannot be answered twice`() {
        val answered = session().answer(0, StepOutcome.CORRECT)
        val failure = runCatching { answered.answer(0, StepOutcome.INCORRECT) }.exceptionOrNull()
        assertTrue(failure is StepAlreadyAnswered)
    }

    @Test
    fun `a step that does not exist cannot be answered`() {
        val failure = runCatching { session().answer(9, StepOutcome.CORRECT) }.exceptionOrNull()
        assertTrue(failure is NoSuchStep)
    }

    @Test
    fun `answering leaves the other steps alone`() {
        val answered = session().answer(1, StepOutcome.SKIPPED)
        assertNull(answered.step(0).outcome)
        assertEquals(StepOutcome.SKIPPED, answered.step(1).outcome)
        assertNull(answered.step(2).outcome)
    }

    @Test
    fun `a session is complete only when every step has an outcome`() {
        var running = session()
        assertFalse(running.isComplete)
        running = running.answer(0, StepOutcome.CORRECT).answer(1, StepOutcome.INCORRECT)
        assertFalse(running.isComplete)
        running = running.answer(2, StepOutcome.SKIPPED)
        assertTrue(running.isComplete)
        assertNull(running.currentStep)
    }

    @Test
    fun `the tallies are derived from the steps, not counted by hand`() {
        val done = session(4)
            .answer(0, StepOutcome.CORRECT)
            .answer(1, StepOutcome.CORRECT, tipUsed = true)
            .answer(2, StepOutcome.INCORRECT)
            .answer(3, StepOutcome.SKIPPED)

        assertEquals(2, done.correctCount)
        assertEquals(1, done.incorrectCount)
        assertEquals(1, done.skippedCount)
        assertEquals(1, done.tipsUsedCount)
    }

    @Test
    fun `the expected answer belongs to the session, not the caller`() {
        assertEquals("word1", session().step(1).expectedAnswer)
    }
}
