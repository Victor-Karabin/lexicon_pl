package com.lexicon.boundary

data class CourseBoundary(
    val id: String,
    val order: Int,
    val level: String,
    val title: Map<String, String>,
    val lessons: List<LessonSummaryBoundary>,
)

data class LessonSummaryBoundary(
    val id: String,
    val courseId: String,
    val number: Int,
    val title: String,
    val wordCount: Int,
    val isCompleted: Boolean,
)

data class LessonBoundary(
    val id: String,
    val courseId: String,
    val number: Int,
    val title: String,
    val vocabularyIds: List<Long>,
    val audio: List<LessonAudioBoundary>,
    val exercises: List<LessonExerciseBoundary>,
    val isCompleted: Boolean,
)

data class LessonAudioBoundary(
    val file: String,
    val section: String?,
    val task: Int,
    val part: String?,
    val remoteId: String?,
)

data class LessonExerciseBoundary(
    val id: String,
    val type: String,
    val instruction: String,
    val audioFile: String?,
    val items: List<ExerciseItemBoundary>,
)

data class ExerciseItemBoundary(
    val label: String?,
    val prompt: String?,
    val options: List<String>,
    val answers: List<String>,
)
