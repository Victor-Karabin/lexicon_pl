package com.lexicon.application.course

import com.lexicon.boundary.LessonSummaryBoundary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonUnlockRuleTest {
    private fun lesson(
        number: Int,
        isCompleted: Boolean,
    ) = LessonSummaryBoundary(
        id = "krok-a1-%02d".format(number),
        courseId = "krok-a1",
        number = number,
        title = "Lesson $number",
        wordCount = 8,
        isCompleted = isCompleted,
    )

    @Test
    fun `the first lesson of a course is always open`() {
        val lessons = listOf(lesson(1, isCompleted = false), lesson(2, isCompleted = false))

        assertTrue(LessonUnlockRule.isUnlocked(lessons, 0))
    }

    @Test
    fun `a lesson stays locked while the one before it is unfinished`() {
        val lessons = listOf(lesson(1, isCompleted = false), lesson(2, isCompleted = false))

        assertFalse(LessonUnlockRule.isUnlocked(lessons, 1))
    }

    @Test
    fun `finishing a lesson opens the next one`() {
        val lessons = listOf(lesson(1, isCompleted = true), lesson(2, isCompleted = false))

        assertTrue(LessonUnlockRule.isUnlocked(lessons, 1))
    }

    @Test
    fun `a lesson two ahead stays locked`() {
        val lessons = listOf(
            lesson(1, isCompleted = true),
            lesson(2, isCompleted = false),
            lesson(3, isCompleted = false),
        )

        assertFalse(LessonUnlockRule.isUnlocked(lessons, 2))
    }

    @Test
    fun `a completed lesson can be revisited even out of order`() {
        val lessons = listOf(lesson(1, isCompleted = false), lesson(2, isCompleted = true))

        assertTrue(LessonUnlockRule.isUnlocked(lessons, 1))
    }

    @Test
    fun `an index past the end is not unlocked`() {
        assertFalse(LessonUnlockRule.isUnlocked(listOf(lesson(1, isCompleted = true)), 5))
    }
}
