package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY sortOrder")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM lessons ORDER BY courseId, number")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lesson_progress")
    fun observeProgress(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLesson(lessonId: String): LessonEntity?

    @Query("SELECT * FROM lesson_exercises WHERE lessonId = :lessonId ORDER BY position")
    suspend fun getExercises(lessonId: String): List<LessonExerciseEntity>

    @Query(
        """
        SELECT i.* FROM lesson_exercise_items i
        INNER JOIN lesson_exercises e ON e.id = i.exerciseId
        WHERE e.lessonId = :lessonId
        ORDER BY i.exerciseId, i.position
        """,
    )
    suspend fun getExerciseItems(lessonId: String): List<LessonExerciseItemEntity>

    @Query("SELECT * FROM lesson_audio WHERE lessonId = :lessonId ORDER BY position")
    suspend fun getAudio(lessonId: String): List<LessonAudioEntity>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getProgress(lessonId: String): LessonProgressEntity?

    /**
     * Words a lesson introduces, skipping any the learner has deleted, the way
     * [PresetDao.getWordIds] does for presets.
     */
    @Query(
        """
        SELECT lv.wordId FROM lesson_vocabulary lv
        INNER JOIN words w ON w.id = lv.wordId
        WHERE lv.lessonId = :lessonId AND w.isDeleted = 0
        ORDER BY lv.position
        """,
    )
    suspend fun getWordIds(lessonId: String): List<Long>

    @Query(
        """
        SELECT lv.* FROM lesson_vocabulary lv
        INNER JOIN words w ON w.id = lv.wordId
        WHERE w.isDeleted = 0
        ORDER BY lv.lessonId, lv.position
        """,
    )
    fun observeLessonWords(): Flow<List<LessonWordEntity>>

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun countLessons(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: LessonProgressEntity)

    /**
     * Replaces the whole book-derived catalogue. Progress is a separate table and
     * is deliberately not touched: it is the learner's, not the asset's.
     */
    @Transaction
    suspend fun replaceCatalog(
        courses: List<CourseEntity>,
        lessons: List<LessonEntity>,
        words: List<LessonWordEntity>,
        audio: List<LessonAudioEntity>,
        exercises: List<LessonExerciseEntity>,
        exerciseItems: List<LessonExerciseItemEntity>,
    ) {
        clearExerciseItems()
        clearExercises()
        clearAudio()
        clearLessonWords()
        clearLessons()
        clearCourses()
        insertCourses(courses)
        insertLessons(lessons)
        insertLessonWords(words)
        insertAudio(audio)
        insertExercises(exercises)
        insertExerciseItems(exerciseItems)
    }

    @Query("DELETE FROM lesson_exercise_items")
    suspend fun clearExerciseItems()

    @Query("DELETE FROM lesson_exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM lesson_audio")
    suspend fun clearAudio()

    @Query("DELETE FROM lesson_vocabulary")
    suspend fun clearLessonWords()

    @Query("DELETE FROM lessons")
    suspend fun clearLessons()

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonWords(words: List<LessonWordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audio: List<LessonAudioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<LessonExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseItems(items: List<LessonExerciseItemEntity>)
}
