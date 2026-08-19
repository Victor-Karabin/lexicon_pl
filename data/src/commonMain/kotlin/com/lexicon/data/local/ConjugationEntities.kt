package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "conjugation_verb")
data class ConjugationVerbEntity(
    @PrimaryKey val infinitive: String,
    val translation: String,
    val formsJson: String,
)

@Entity(tableName = "conjugation_course")
data class ConjugationCourseEntity(
    @PrimaryKey val id: String,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "conjugation_course_verb",
    primaryKeys = ["courseId", "infinitive"],
    indices = [Index("courseId")],
)
data class ConjugationCourseVerbEntity(
    val courseId: String,
    val infinitive: String,
)

@Entity(
    tableName = "conjugation_progress",
    primaryKeys = ["courseId", "infinitive", "person"],
    indices = [Index("courseId")],
)
data class ConjugationProgressEntity(
    val courseId: String,
    val infinitive: String,
    val person: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)

@Dao
interface ConjugationDao {
    @Query("SELECT COUNT(*) FROM conjugation_verb")
    suspend fun countVerbs(): Int

    @Query("SELECT * FROM conjugation_verb ORDER BY infinitive")
    suspend fun verbs(): List<ConjugationVerbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVerbs(rows: List<ConjugationVerbEntity>)

    @Query("DELETE FROM conjugation_verb WHERE infinitive = :infinitive")
    suspend fun deleteVerb(infinitive: String)

    @Query("SELECT * FROM conjugation_course ORDER BY createdAtEpochMillis")
    suspend fun courses(): List<ConjugationCourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCourse(row: ConjugationCourseEntity)

    @Query("DELETE FROM conjugation_course WHERE id = :courseId")
    suspend fun deleteCourse(courseId: String)

    @Query("SELECT infinitive FROM conjugation_course_verb WHERE courseId = :courseId ORDER BY infinitive")
    suspend fun courseVerbs(courseId: String): List<String>

    @Query("SELECT * FROM conjugation_course_verb ORDER BY courseId, infinitive")
    suspend fun allCourseVerbs(): List<ConjugationCourseVerbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCourseVerbs(rows: List<ConjugationCourseVerbEntity>)

    @Query("DELETE FROM conjugation_course_verb WHERE courseId = :courseId")
    suspend fun clearCourseVerbs(courseId: String)

    @Query("SELECT * FROM conjugation_progress WHERE courseId = :courseId")
    suspend fun progress(courseId: String): List<ConjugationProgressEntity>

    @Query(
        "SELECT * FROM conjugation_progress " +
            "WHERE courseId = :courseId AND infinitive = :infinitive AND person = :person",
    )
    suspend fun progressFor(
        courseId: String,
        infinitive: String,
        person: String,
    ): ConjugationProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: ConjugationProgressEntity)

    @Query("DELETE FROM conjugation_progress WHERE courseId = :courseId")
    suspend fun clearProgress(courseId: String)
}
