package com.lexicon.data.repository

import com.lexicon.boundary.CourseBoundary
import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.LessonBoundary
import com.lexicon.boundary.LessonSummaryBoundary
import com.lexicon.boundary.SyncOutcomeBoundary
import com.lexicon.data.local.CourseDao
import com.lexicon.data.local.CourseSeeder
import com.lexicon.data.local.LessonProgressEntity
import com.lexicon.data.local.decodeLocalized
import com.lexicon.data.local.toBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl
    @Inject
    constructor(
        private val courseDao: CourseDao,
        private val seeder: CourseSeeder,
    ) : CourseRepository {
        override suspend fun syncFromSource(): SyncOutcomeBoundary = seeder.sync()

        override fun observeCourses(): Flow<List<CourseBoundary>> =
            combine(
                courseDao.observeCourses(),
                courseDao.observeLessons(),
                courseDao.observeLessonWords(),
                courseDao.observeProgress(),
            ) { courses, lessons, words, progress ->
                val wordsByLesson = words.groupBy { it.lessonId }
                val completed = progress.filter { it.isCompleted }.map { it.lessonId }.toSet()
                val lessonsByCourse = lessons.groupBy { it.courseId }

                courses.map { course ->
                    CourseBoundary(
                        id = course.id,
                        order = course.sortOrder,
                        level = course.level,
                        title = course.titleJson.decodeLocalized(),
                        lessons = lessonsByCourse[course.id].orEmpty().map { lesson ->
                            LessonSummaryBoundary(
                                id = lesson.id,
                                courseId = lesson.courseId,
                                number = lesson.number,
                                title = lesson.title,
                                wordCount = wordsByLesson[lesson.id].orEmpty().size,
                                isCompleted = lesson.id in completed,
                            )
                        },
                    )
                }
            }.onStart { seeder.ensureSeeded() }

        override suspend fun getLesson(lessonId: String): LessonBoundary? {
            seeder.ensureSeeded()
            val lesson = courseDao.getLesson(lessonId) ?: return null
            return lesson.toBoundary(
                wordIds = courseDao.getWordIds(lessonId),
                audio = courseDao.getAudio(lessonId),
                isCompleted = courseDao.getProgress(lessonId)?.isCompleted == true,
            )
        }

        override suspend fun getLessonWordIds(lessonId: String): List<Long> {
            seeder.ensureSeeded()
            return courseDao.getWordIds(lessonId)
        }

        override suspend fun setLessonCompleted(
            lessonId: String,
            isCompleted: Boolean,
        ) {
            courseDao.upsertProgress(
                LessonProgressEntity(
                    lessonId = lessonId,
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) System.currentTimeMillis() else null,
                ),
            )
        }

        override suspend fun countLessons(): Int {
            seeder.ensureSeeded()
            return courseDao.countLessons()
        }
    }
