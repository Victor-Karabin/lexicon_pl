package com.lexicon.interactors.course

import com.lexicon.interactors.presets.LocalizedText
import kotlinx.collections.immutable.ImmutableList

data class Course(
    val id: CourseId,
    val order: Int,
    val level: String,
    val title: LocalizedText,
    val lessons: ImmutableList<LessonSummary>,
)

data class CourseId(val value: String)

val Course.completedCount: Int get() = lessons.count { it.isCompleted }

val Course.currentLesson: LessonSummary? get() = lessons.firstOrNull { !it.isCompleted }
