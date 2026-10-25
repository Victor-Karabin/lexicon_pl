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
    val example: String = "",
    val isUserCreated: Boolean = false,
    val aspect: String = "",
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
    override val attempted: Int,
    override val correct: Int,
    override val incorrect: Int,
    override val streak: Int,
) : AttemptRecord

@Dao
interface ConjugationDao {
    @Query("SELECT COUNT(*) FROM conjugation_verb")
    suspend fun countVerbs(): Int

    @Query("SELECT COUNT(*) FROM conjugation_verb WHERE isUserCreated = 0")
    suspend fun countBundledVerbs(): Int

    @Query("SELECT * FROM conjugation_verb WHERE infinitive = :infinitive LIMIT 1")
    suspend fun verb(infinitive: String): ConjugationVerbEntity?

    @Query("DELETE FROM conjugation_verb WHERE infinitive = :infinitive AND isUserCreated = 1")
    suspend fun deleteUserVerb(infinitive: String)

    @Query("SELECT * FROM conjugation_verb ORDER BY infinitive")
    suspend fun verbs(): List<ConjugationVerbEntity>

    @Query(
        """
        SELECT * FROM conjugation_verb
        WHERE :needle = '' OR instr(lower(infinitive), lower(:needle)) > 0 OR instr(lower(translation), lower(:needle)) > 0
        ORDER BY infinitive
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun verbPage(
        needle: String,
        limit: Int,
        offset: Int,
    ): List<ConjugationVerbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVerbs(rows: List<ConjugationVerbEntity>)

    @Query("DELETE FROM conjugation_verb WHERE infinitive = :infinitive")
    suspend fun deleteVerb(infinitive: String)

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
}
