package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    suspend fun syncFromSource(): SyncOutcomeBoundary

    fun observeCourses(): Flow<List<CourseBoundary>>

    suspend fun getLesson(lessonId: String): LessonBoundary?

    suspend fun getLessonWordIds(lessonId: String): List<Long>

    suspend fun setLessonCompleted(
        lessonId: String,
        isCompleted: Boolean,
    )

    suspend fun countLessons(): Int
}
