package com.lexicon.domain.course

import com.lexicon.boundary.LessonSummaryBoundary

object LessonUnlockRule {
    fun isUnlocked(
        lessons: List<LessonSummaryBoundary>,
        index: Int,
    ): Boolean {
        val lesson = lessons.getOrNull(index) ?: return false
        return index == 0 || lesson.isCompleted || lessons[index - 1].isCompleted
    }
}
