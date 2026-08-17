package com.lexicon.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulingTest {
    private val settings = ReviewSettings()

    private fun ReviewState.answer(
        quality: RecallQuality,
        today: Long,
    ) = next(quality, today, settings)

    @Test
    fun `a new word climbs the ladder one, six, then by its ease`() {
        var state = ReviewState()

        state = state.answer(RecallQuality.PERFECT, today = 0)
        assertEquals(1, state.repetitions)
        assertEquals(1L, state.intervalDays)
        assertEquals(1L, state.dueAtEpochDay)

        state = state.answer(RecallQuality.PERFECT, today = 1)
        assertEquals(6L, state.intervalDays)
        assertEquals(7L, state.dueAtEpochDay)

        state = state.answer(RecallQuality.PERFECT, today = 7)
        assertEquals(2.8, state.easeFactor, 0.0001)
        assertEquals(17L, state.intervalDays)
        assertEquals(7 + 17L, state.dueAtEpochDay)
    }

    @Test
    fun `answering perfectly raises the ease, stumbling lowers it`() {
        val perfect = ReviewState().answer(RecallQuality.PERFECT, today = 0)
        assertTrue("a perfect answer should not cost ease", perfect.easeFactor > 2.5)

        val hesitant = ReviewState().answer(RecallQuality.HESITANT, today = 0)
        assertTrue("needing a tip should cost ease", hesitant.easeFactor < 2.5)
    }

    @Test
    fun `ease never falls below the floor, however often a word is missed`() {
        var state = ReviewState()
        repeat(50) { state = state.answer(RecallQuality.FORGOTTEN, today = it.toLong()) }
        assertEquals(settings.minimumEase, state.easeFactor, 0.0001)
    }

    @Test
    fun `a forgotten word restarts the ladder rather than going back to unseen`() {
        var state = ReviewState()
        repeat(4) { state = state.answer(RecallQuality.PERFECT, today = it.toLong()) }
        val grown = state.intervalDays
        assertTrue(grown > settings.secondIntervalDays)

        state = state.answer(RecallQuality.FORGOTTEN, today = 30)
        assertEquals(0, state.repetitions)
        assertEquals(settings.firstIntervalDays, state.intervalDays)
        assertEquals(1, state.lapses)

        assertTrue(state.isLearned)
    }

    @Test
    fun `a skipped word is treated as forgotten, since not saying is not knowing`() {
        val skipped = ReviewState(repetitions = 3, intervalDays = 20).answer(RecallQuality.SKIPPED, today = 20)
        assertEquals(0, skipped.repetitions)
        assertEquals(1, skipped.lapses)
    }

    @Test
    fun `a tip keeps the word moving forward, but only just`() {
        val state = ReviewState().answer(RecallQuality.HESITANT, today = 0)
        assertEquals("a tipped answer still counts as recalled", 1, state.repetitions)
        assertEquals(0, state.lapses)
    }

    @Test
    fun `intervals stay within their bounds`() {
        val tight = ReviewSettings(maxIntervalDays = 10)
        var state = ReviewState()
        repeat(10) { state = state.next(RecallQuality.PERFECT, it.toLong(), tight) }
        assertTrue(state.intervalDays <= tight.maxIntervalDays)
    }

    @Test
    fun `a word is mastered once its interval outgrows the threshold`() {
        assertFalse(ReviewState(intervalDays = 20).isMastered(settings))
        assertTrue(ReviewState(intervalDays = 21).isMastered(settings))
        assertTrue(ReviewState(intervalDays = 90).isMastered(settings))
    }

    @Test
    fun `an unanswered word is not learned`() {
        assertFalse(ReviewState().isLearned)
        assertTrue(ReviewState(repetitions = 1).isLearned)
    }

    @Test
    fun `the due queue comes back most overdue first`() {
        val schedule = mapOf(
            "barely" to ReviewState(dueAtEpochDay = 10),
            "ancient" to ReviewState(dueAtEpochDay = 2),
            "middling" to ReviewState(dueAtEpochDay = 6),
            "not yet" to ReviewState(dueAtEpochDay = 30),
        )

        assertEquals(listOf("ancient", "middling", "barely"), schedule.dueOn(todayEpochDay = 10))
        assertEquals(listOf("ancient", "middling"), schedule.dueOn(todayEpochDay = 10, limit = 2))
        assertEquals(emptyList<String>(), schedule.dueOn(todayEpochDay = 0))
    }
}
