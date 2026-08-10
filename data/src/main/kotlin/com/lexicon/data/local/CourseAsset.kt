package com.lexicon.data.local

import kotlinx.serialization.Serializable

@Serializable
data class CourseCatalogAsset(
    val courses: List<CourseAsset> = emptyList(),
)

@Serializable
data class CourseAsset(
    val id: String,
    val order: Int = 0,
    val level: String = "",
    val title: Map<String, String> = emptyMap(),
    val lessons: List<LessonAsset> = emptyList(),
)

@Serializable
data class LessonAsset(
    val id: String,
    val courseId: String,
    val number: Int,
    val title: String,
    val sections: List<LessonSectionAsset> = emptyList(),
    val vocabularyIds: List<Long> = emptyList(),
    val audio: List<LessonAudioAsset> = emptyList(),
    val workbookAudio: List<LessonAudioAsset> = emptyList(),
)

@Serializable
data class LessonSectionAsset(
    val letter: String,
    val title: String,
)

@Serializable
data class LessonAudioAsset(
    val file: String,
    val section: String? = null,
    val task: Int = 0,
    val part: String? = null,
)
