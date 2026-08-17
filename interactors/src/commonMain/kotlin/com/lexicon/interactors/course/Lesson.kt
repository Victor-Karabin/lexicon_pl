package com.lexicon.interactors.course

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

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

data class LessonId(val value: String)
