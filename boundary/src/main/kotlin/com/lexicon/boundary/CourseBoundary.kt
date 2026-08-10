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
    val sections: List<LessonSectionBoundary>,
    val vocabularyIds: List<Long>,
    val audio: List<LessonAudioBoundary>,
    val isCompleted: Boolean,
)

data class LessonSectionBoundary(
    val letter: String,
    val title: String,
)

data class LessonAudioBoundary(
    val file: String,
    val source: String,
    val section: String?,
    val task: Int,
    val part: String?,
    val remoteId: String?,
)
