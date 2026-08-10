package com.lexicon.domain.course

import com.lexicon.boundary.LessonSummaryBoundary

/**
 * Which lessons a learner may open.
 *
 * A course is an ordered path, so a lesson opens once the one before it is done.
 * The first lesson of every course is always open, and a completed lesson stays
 * open so it can be revisited. Keeping this in one place stops the rule being
 * re-derived, slightly differently, in the list and on the lesson screen.
 */
object LessonUnlockRule {
    fun isUnlocked(
        lessons: List<LessonSummaryBoundary>,
        index: Int,
    ): Boolean {
        val lesson = lessons.getOrNull(index) ?: return false
        return index == 0 || lesson.isCompleted || lessons[index - 1].isCompleted
    }
}
