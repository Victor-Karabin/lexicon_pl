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
    val vocabularyIds: ImmutableList<VocabularyId>,
    val audio: ImmutableList<LessonAudio>,
    val exercises: ImmutableList<LessonExercise>,
    val isCompleted: Boolean,
)

data class LessonAudio(
    val file: String,
    val section: String?,
    val task: Int,
    val part: String?,
    val remoteId: String?,
)

val Course.completedCount: Int get() = lessons.count { it.isCompleted }

val Course.currentLesson: LessonSummary? get() = lessons.firstOrNull { !it.isCompleted }

val LessonAudio.label: String
    get() = buildString {
        section?.let { append(it) }
        append(task)
        part?.let { append('.').append(it) }
    }

/**
 * An exercise from the book. Each kind asks something different of the learner,
 * so they are separate types rather than one shape with unused fields.
 */
sealed interface LessonExercise {
    val id: String
    val instruction: String
    val audioFile: String?

    /** Listen and read along; there is nothing to mark. */
    data class Repeat(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val words: ImmutableList<String>,
    ) : LessonExercise

    /** Listen, then pick which of two near-identical words was said. */
    data class MinimalPair(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<MinimalPairItem>,
    ) : LessonExercise

    /** Listen, then type what belongs in each blank. */
    data class GapFill(
        override val id: String,
        override val instruction: String,
        override val audioFile: String?,
        val items: ImmutableList<GapFillItem>,
    ) : LessonExercise
}

data class MinimalPairItem(
    val label: String,
    val options: ImmutableList<String>,
    val answer: String,
)

data class GapFillItem(
    val prompt: String,
    val answers: ImmutableList<String>,
)

val LessonExercise.questionCount: Int
    get() = when (this) {
        is LessonExercise.Repeat -> words.size
        is LessonExercise.MinimalPair -> items.size
        is LessonExercise.GapFill -> items.sumOf { it.answers.size }
    }
