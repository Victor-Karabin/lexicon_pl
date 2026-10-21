package com.lexicon.model.scheduling

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
        val threshold = settings.masteredIntervalDays
        assertFalse(ReviewState(intervalDays = threshold - 1).isMastered(settings))
        assertTrue(ReviewState(intervalDays = threshold).isMastered(settings))
        assertTrue(ReviewState(intervalDays = threshold * 4).isMastered(settings))
    }

    @Test
    fun `the due queue comes back most overdue first`() {
        val schedule = mapOf(
            "barely" to scheduled(dueAt = 10),
            "ancient" to scheduled(dueAt = 2),
            "middling" to scheduled(dueAt = 6),
            "not yet" to scheduled(dueAt = 30),
        )

        assertEquals(listOf("ancient", "middling", "barely"), schedule.dueOn(todayEpochDay = 10))
        assertEquals(listOf("ancient", "middling"), schedule.dueOn(todayEpochDay = 10, limit = 2))
        assertEquals(emptyList<String>(), schedule.dueOn(todayEpochDay = 0))
    }

    @Test
    fun `words already reviewed today come first in the queue, then the most overdue`() {
        val schedule = mapOf(
            "overdue a week" to scheduled(dueAt = TODAY - 7),
            "answered today" to ReviewState().answer(RecallQuality.PERFECT, TODAY),
            "overdue a day" to scheduled(dueAt = TODAY - 1),
            "not yet" to scheduled(dueAt = TODAY + 5),
            "forgotten today" to ReviewState(repetitions = 3, intervalDays = 20).answer(RecallQuality.FORGOTTEN, TODAY),
            "due today" to scheduled(dueAt = TODAY),
        )

        val queue = schedule.dueOn(TODAY)

        assertEquals(setOf("answered today", "forgotten today"), queue.take(2).toSet())
        assertEquals(listOf("overdue a week", "overdue a day", "due today"), queue.drop(2))
    }

    @Test
    fun `the queue limit counts words reviewed today before it reaches the overdue ones`() {
        val schedule = mapOf(
            "overdue" to scheduled(dueAt = TODAY - 3),
            "answered today" to ReviewState().answer(RecallQuality.PERFECT, TODAY),
            "forgotten today" to ReviewState().answer(RecallQuality.FORGOTTEN, TODAY),
        )

        assertEquals(setOf("answered today", "forgotten today"), schedule.dueOn(TODAY, limit = 2).toSet())
        assertEquals(listOf("overdue"), schedule.dueOn(TODAY, limit = 3).drop(2))
        assertEquals(1, schedule.dueOn(TODAY, limit = 1).size)
        assertTrue(schedule.dueOn(TODAY, limit = 1).single() in setOf("answered today", "forgotten today"))
        assertEquals(emptyList<String>(), schedule.dueOn(TODAY, limit = 0))
    }

    @Test
    fun `a word is reviewed today after a correct answer and after a lapse, but not the day after`() {
        val answered = ReviewState().answer(RecallQuality.PERFECT, TODAY)
        val lapsed = ReviewState(repetitions = 3, intervalDays = 20).answer(RecallQuality.FORGOTTEN, TODAY)

        assertTrue(answered.isReviewedOn(TODAY))
        assertTrue(lapsed.isReviewedOn(TODAY))
        assertFalse(answered.isReviewedOn(TODAY + 1))
        assertFalse(lapsed.isReviewedOn(TODAY + 1))
        assertFalse(ReviewState().answer(RecallQuality.PERFECT, TODAY - 1).isReviewedOn(TODAY))
        assertFalse(ReviewState().answer(RecallQuality.FORGOTTEN, TODAY - 1).isReviewedOn(TODAY))
    }

    @Test
    fun `a word is still reviewed today when its interval was clamped at the maximum`() {
        val tight = ReviewSettings(maxIntervalDays = 10)
        var state = ReviewState()
        repeat(10) { state = state.next(RecallQuality.PERFECT, it.toLong(), tight) }
        val lastAnsweredOn = 9L

        assertEquals(tight.maxIntervalDays, state.intervalDays)
        assertTrue(state.isReviewedOn(lastAnsweredOn))
        assertFalse(state.isReviewedOn(lastAnsweredOn - 1))
        assertFalse(state.isReviewedOn(lastAnsweredOn + 1))
    }

    @Test
    fun `a word is due on the day its interval ends and every day after, never before`() {
        val state = ReviewState().answer(RecallQuality.PERFECT, TODAY)

        assertFalse(state.isDueOn(TODAY))
        assertFalse(state.isDueOn(state.dueAtEpochDay - 1))
        assertTrue(state.isDueOn(state.dueAtEpochDay))
        assertTrue(state.isDueOn(state.dueAtEpochDay + 30))
    }

    private fun scheduled(dueAt: Long) = ReviewState(intervalDays = 1, dueAtEpochDay = dueAt)

    private companion object {
        const val TODAY = 100L
    }
}
