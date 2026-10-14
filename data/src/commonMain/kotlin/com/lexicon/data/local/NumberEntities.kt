package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

const val NUMBER_COURSE_ROW = 0

@Entity(tableName = "number_course")
data class NumberCourseEntity(
    @PrimaryKey val id: Int = NUMBER_COURSE_ROW,
    val spec: String,
)

@Entity(tableName = "number_progress", primaryKeys = ["value", "kind"])
data class NumberProgressEntity(
    val value: Long,
    val kind: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)

@Dao
interface NumberDao {
    @Query("SELECT * FROM number_course WHERE id = :id LIMIT 1")
    suspend fun course(id: Int = NUMBER_COURSE_ROW): NumberCourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCourse(row: NumberCourseEntity)

    @Query("SELECT * FROM number_progress")
    suspend fun progress(): List<NumberProgressEntity>

    @Query("SELECT * FROM number_progress WHERE value = :value AND kind = :kind LIMIT 1")
    suspend fun progressFor(
        value: Long,
        kind: String,
    ): NumberProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: NumberProgressEntity)
}
