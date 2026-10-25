package com.lexicon.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val RESET = "krok-a1-01"
private const val KEPT = "krok-a1-02"

@RunWith(AndroidJUnit4::class)
class CourseDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var course: CourseDao

    @Before
    fun open() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        course = database.courseDao()
    }

    @After
    fun close() = database.close()

    private suspend fun finish(lessonId: String) {
        course.upsertProgress(LessonProgressEntity(lessonId, isCompleted = true, completedAt = 1L))
        course.upsertScriptProgress(LessonScriptProgressEntity(lessonId, "{}"))
        course.upsertLastResult(LessonLastResultEntity(lessonId, "{}"))
    }

    @Test
    fun resettingALessonClearsItsProgressAnswersAndLastResultOnly() =
        runTest {
            finish(RESET)
            finish(KEPT)

            course.resetLesson(RESET)

            assertNull(course.getProgress(RESET))
            assertNull(course.getScriptProgress(RESET))
            assertNull(course.getLastResult(RESET))
            assertNotNull(course.getProgress(KEPT))
            assertNotNull(course.getScriptProgress(KEPT))
            assertNotNull(course.getLastResult(KEPT))
        }
}
