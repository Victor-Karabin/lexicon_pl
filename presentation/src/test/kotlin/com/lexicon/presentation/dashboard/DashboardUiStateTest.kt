package com.lexicon.presentation.dashboard

import com.lexicon.model.vocabularycourse.CourseProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardUiStateTest {
    @Test
    fun `the home card shows the words due that progress counted`() {
        assertEquals(7, DashboardUiState(progress = CourseProgress(dueWords = 7)).dueWords)
    }

    @Test
    fun `the home card shows no due part while progress is not loaded or counted none`() {
        assertNull(DashboardUiState(progress = null).dueWords)
        assertNull(DashboardUiState(progress = CourseProgress()).dueWords)
        assertNull(DashboardUiState(progress = CourseProgress(dueWords = 0)).dueWords)
    }
}
