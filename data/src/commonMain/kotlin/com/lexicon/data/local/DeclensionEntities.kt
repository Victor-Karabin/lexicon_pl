package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "declension_progress",
    primaryKeys = ["courseId", "word", "column", "grammaticalCase"],
    indices = [Index("courseId")],
)
data class DeclensionProgressEntity(
    val courseId: String,
    val word: String,
    val column: String,
    val grammaticalCase: String,
    override val attempted: Int,
    override val correct: Int,
    override val incorrect: Int,
    override val streak: Int,
) : AttemptRecord

@Dao
interface DeclensionDao {
    @Query(
        """
        SELECT * FROM words
        WHERE isDeleted = 0
          AND (declension != '' OR (adjectiveForms != '' AND text NOT LIKE '% %'))
          AND instr(searchKey, :foldedQuery) > 0
        ORDER BY text
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun declinableWords(
        foldedQuery: String,
        limit: Int,
        offset: Int,
    ): List<WordEntity>

    @Query(
        """
        SELECT * FROM words
        WHERE isDeleted = 0
          AND status IN ('TO_LEARN', 'FAVOURITE')
          AND (declension != '' OR (adjectiveForms != '' AND text NOT LIKE '% %'))
        ORDER BY text
        """,
    )
    suspend fun studySetWords(): List<WordEntity>

    @Query("SELECT * FROM declension_progress WHERE courseId = :courseId")
    suspend fun progress(courseId: String): List<DeclensionProgressEntity>

    @Query(
        "SELECT * FROM declension_progress " +
            "WHERE courseId = :courseId AND word = :word AND `column` = :column AND grammaticalCase = :case LIMIT 1",
    )
    suspend fun progressFor(
        courseId: String,
        word: String,
        column: String,
        case: String,
    ): DeclensionProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: DeclensionProgressEntity)
}
