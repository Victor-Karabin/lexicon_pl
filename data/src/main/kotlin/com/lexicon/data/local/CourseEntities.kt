package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val level: String,
    val titleJson: String,
)

@Entity(tableName = "lessons", indices = [Index("courseId")])
data class LessonEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val number: Int,
    val title: String,
)

@Entity(
    tableName = "lesson_vocabulary",
    primaryKeys = ["lessonId", "wordId"],
    indices = [Index("lessonId"), Index("wordId")],
)
data class LessonWordEntity(
    val lessonId: String,
    val wordId: Long,
    val position: Int,
)

@Entity(
    tableName = "lesson_audio",
    primaryKeys = ["lessonId", "file"],
    indices = [Index("lessonId")],
)
data class LessonAudioEntity(
    val lessonId: String,
    val file: String,
    val section: String?,
    val task: Int,
    val part: String?,
    val position: Int,
    /** Google Drive file id, when the track can be fetched rather than side-loaded. */
    val remoteId: String?,
)

/**
 * What the learner has done. Unlike everything above it is not derived from the
 * asset, so [CourseSeeder] leaves it alone when the catalogue is replaced.
 */
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val isCompleted: Boolean,
    val completedAt: Long?,
)

@Entity(tableName = "lesson_exercises", indices = [Index("lessonId")])
data class LessonExerciseEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val tag: String,
    val type: String,
    val instruction: String,
    val audioFile: String?,
    val position: Int,
)

/**
 * One question of an exercise. The columns a row uses depend on its exercise's
 * type; [itemsJson] would have been the alternative, but keeping the answer in a
 * column is what lets the build fail on an exercise with no answers.
 */
@Entity(
    tableName = "lesson_exercise_items",
    primaryKeys = ["exerciseId", "position"],
    indices = [Index("exerciseId")],
)
data class LessonExerciseItemEntity(
    val exerciseId: String,
    val position: Int,
    val label: String?,
    val prompt: String?,
    val optionsJson: String,
    val answersJson: String,
)
