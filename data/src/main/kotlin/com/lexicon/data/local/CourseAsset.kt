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
    val vocabularyIds: List<Long> = emptyList(),
    val audio: List<LessonAudioAsset> = emptyList(),
    val exercises: List<LessonExerciseAsset> = emptyList(),
)

@Serializable
data class LessonAudioAsset(
    val file: String,
    val section: String? = null,
    val task: Int = 0,
    val part: String? = null,
    val remoteId: String? = null,
)

@Serializable
data class LessonExerciseAsset(
    val id: String,
    val type: String,
    val instruction: String,
    val audioFile: String? = null,
    val items: List<ExerciseItemAsset> = emptyList(),
)

/**
 * One question. Which fields carry meaning depends on the exercise type: a
 * minimal pair uses [options] and [answer], a gap-fill uses [prompt] and
 * [answers].
 */
@Serializable
data class ExerciseItemAsset(
    val label: String? = null,
    val text: String? = null,
    val prompt: String? = null,
    val options: List<String> = emptyList(),
    val answer: String? = null,
    val answers: List<String> = emptyList(),
)
