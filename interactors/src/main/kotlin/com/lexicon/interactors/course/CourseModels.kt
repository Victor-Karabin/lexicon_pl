package com.lexicon.interactors.course

import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

@JvmInline
value class LessonId(val value: String)

@JvmInline
value class CourseId(val value: String)

data class Course(
    val id: CourseId,
    val order: Int,
    val level: String,
    val title: LocalizedText,
    val lessons: ImmutableList<LessonSummary>,
)

data class LessonSummary(
    val id: LessonId,
    val number: Int,
    val title: String,
    val wordCount: Int,
    val isCompleted: Boolean,
    val isUnlocked: Boolean,
)

data class Lesson(
    val id: LessonId,
    val courseId: CourseId,
    val number: Int,
    val title: String,
    val sections: ImmutableList<LessonSection>,
    val vocabularyIds: ImmutableList<VocabularyId>,
    val audio: ImmutableList<LessonAudio>,
    val isCompleted: Boolean,
)

data class LessonSection(
    val letter: String,
    val title: String,
)

data class LessonAudio(
    val file: String,
    val source: LessonAudioSource,
    val section: String?,
    val task: Int,
    val part: String?,
    val remoteId: String?,
)

enum class LessonAudioSource { COURSEBOOK, WORKBOOK }

val Course.completedCount: Int get() = lessons.count { it.isCompleted }

val Course.currentLesson: LessonSummary? get() = lessons.firstOrNull { !it.isCompleted }

val LessonAudio.label: String
    get() = buildString {
        section?.let { append(it) }
        append(task)
        part?.let { append('.').append(it) }
    }
