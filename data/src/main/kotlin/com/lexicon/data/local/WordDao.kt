package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    /** The study set is exactly the favourited words; nothing else is ever trained on. */
    @Query("SELECT * FROM words WHERE isFavourite = 1 AND isDeleted = 0 ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomForStudy(count: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids) AND isDeleted = 0")
    suspend fun getByIds(ids: List<Long>): List<WordEntity>

    @Query("UPDATE words SET isFavourite = :isFavourite WHERE id IN (:ids)")
    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    @Query("SELECT id FROM words WHERE isFavourite = 1 AND isDeleted = 0")
    fun observeFavouriteIds(): Flow<List<Long>>

    /** Mirrors [getRandomForStudy]'s pool, so the two can never disagree. */
    @Query("SELECT COUNT(*) FROM words WHERE isFavourite = 1 AND isDeleted = 0")
    suspend fun countForStudy(): Int

    /**
     * [foldedQuery] must already be folded; matching a raw query against a folded column is
     * how a search for "zolw" silently stops finding "żółw".
     */
    @Query(
        """
        SELECT * FROM words
        WHERE searchKey LIKE '%' || :foldedQuery || '%'
          AND isDeleted = 0
          AND (:ignoreLevels = 1 OR cefr IN (:levels))
        ORDER BY text
        LIMIT :limit
        """,
    )
    suspend fun search(
        foldedQuery: String,
        levels: List<String>,
        ignoreLevels: Int,
        limit: Int,
    ): List<WordEntity>

    @Update
    suspend fun updateAll(words: List<WordEntity>)

    @Query("SELECT * FROM words")
    suspend fun getAll(): List<WordEntity>

    @Query("DELETE FROM words WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM words WHERE isDeleted = 0")
    suspend fun count(): Int

    /** Includes deleted rows: the reconcile has to see them to keep them deleted. */
    @Query("SELECT COUNT(*) FROM words")
    suspend fun countIncludingDeleted(): Int

    @Query("UPDATE words SET isDeleted = :isDeleted WHERE id = :id")
    suspend fun setDeleted(
        id: Long,
        isDeleted: Boolean,
    )

    @Insert
    suspend fun insertAll(words: List<WordEntity>)
}
