package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "conjugation_selection")
data class ConjugationSelectionEntity(
    @PrimaryKey val infinitive: String,
)

@Entity(
    tableName = "conjugation_progress",
    primaryKeys = ["infinitive", "person"],
    indices = [Index("infinitive")],
)
data class ConjugationProgressEntity(
    val infinitive: String,
    val person: String,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val streak: Int,
)

@Dao
interface ConjugationDao {
    @Query("SELECT infinitive FROM conjugation_selection ORDER BY infinitive")
    suspend fun selection(): List<String>

    @Query("DELETE FROM conjugation_selection")
    suspend fun clearSelection()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToSelection(rows: List<ConjugationSelectionEntity>)

    @Query("SELECT * FROM conjugation_progress")
    suspend fun progress(): List<ConjugationProgressEntity>

    @Query("SELECT * FROM conjugation_progress WHERE infinitive = :infinitive AND person = :person")
    suspend fun progressFor(
        infinitive: String,
        person: String,
    ): ConjugationProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: ConjugationProgressEntity)

    @Query("DELETE FROM conjugation_progress")
    suspend fun clearProgress()
}
