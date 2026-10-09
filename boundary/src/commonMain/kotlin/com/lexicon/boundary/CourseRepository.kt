package com.lexicon.boundary

import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScript
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    suspend fun seedFromAsset(): SeedOutcomeBoundary

    fun observeCourses(): Flow<List<CourseBoundary>>

    suspend fun getLesson(lessonId: String): LessonBoundary?

    suspend fun getLessonWordIds(lessonId: String): List<Long>

    suspend fun setLessonCompleted(
        lessonId: String,
        isCompleted: Boolean,
    )

    suspend fun countLessons(): Int

    suspend fun getLessonScript(lessonId: String): LessonScript?

    suspend fun getLessonScriptProgress(lessonId: String): LessonProgress?

    suspend fun saveLessonScriptProgress(
        lessonId: String,
        progress: LessonProgress,
    )
}
