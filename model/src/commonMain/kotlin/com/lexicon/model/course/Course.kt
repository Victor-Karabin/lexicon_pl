package com.lexicon.model.course

import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.LocalizedText
import kotlinx.collections.immutable.ImmutableList

data class CourseId(val value: String)

data class LessonId(val value: String)

data class LessonSummary(
    val id: LessonId,
    val number: Int,
    val title: String,
    val wordCount: Int,
    val isCompleted: Boolean,
    val isUnlocked: Boolean,
)

data class Course(
    val id: CourseId,
    val order: Int,
    val level: CefrLevel?,
    val title: LocalizedText,
    val lessons: ImmutableList<LessonSummary>,
) {
    val completedCount: Int get() = lessons.count { it.isCompleted }

    val currentLesson: LessonSummary? get() = lessons.firstOrNull { !it.isCompleted }

    val isComplete: Boolean get() = lessons.isNotEmpty() && lessons.all { it.isCompleted }
}
