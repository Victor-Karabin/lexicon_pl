package com.lexicon.application.training

import com.lexicon.interactors.countries.CountryCourse
import com.lexicon.interactors.numbers.NumberCourse
import com.lexicon.interactors.training.DrillCourseProgress
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrillCourseProgressTest {
    @Test
    fun `the fraction is the share mastered and zero when there is nothing to master`() {
        assertEquals(0f, DrillCourseProgress(items = 0, total = 0, mastered = 0, attempted = 0).fraction, 0f)
        assertEquals(0.25f, DrillCourseProgress(items = 2, total = 8, mastered = 2, attempted = 3).fraction, 0f)
    }

    @Test
    fun `it is complete only when something is to be mastered and all of it is`() {
        assertFalse(DrillCourseProgress(items = 0, total = 0, mastered = 0, attempted = 0).isComplete)
        assertFalse(DrillCourseProgress(items = 2, total = 8, mastered = 7, attempted = 8).isComplete)
        assertTrue(DrillCourseProgress(items = 2, total = 8, mastered = 8, attempted = 8).isComplete)
    }

    @Test
    fun `numbers and countries both carry it`() {
        val progress = DrillCourseProgress(items = 2, total = 6, mastered = 1, attempted = 2)
        assertEquals(progress, NumberCourse(spec = "1-2", progress = progress).progress)
        assertEquals(progress, CountryCourse(codes = persistentListOf("AG", "PL"), progress = progress).progress)
    }
}
