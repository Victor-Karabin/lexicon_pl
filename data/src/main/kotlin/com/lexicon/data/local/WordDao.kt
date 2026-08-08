package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    /**
     * Draws from the favourites when the user has any, and from everything otherwise. Done in
     * one statement so the choice cannot race a favourite being toggled between two queries.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE isFavourite = 1 OR (SELECT COUNT(*) FROM words WHERE isFavourite = 1) = 0
        ORDER BY RANDOM() LIMIT :count
        """,
    )
    suspend fun getRandomForStudy(count: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<WordEntity>

    @Query("UPDATE words SET isFavourite = :isFavourite WHERE id IN (:ids)")
    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    @Query("SELECT id FROM words WHERE isFavourite = 1")
    fun observeFavouriteIds(): Flow<List<Long>>

    /** Mirrors [getRandomForStudy]'s choice of pool, so the two can never disagree. */
    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE isFavourite = 1 OR (SELECT COUNT(*) FROM words WHERE isFavourite = 1) = 0
        """,
    )
    suspend fun countForStudy(): Int

    /**
     * [foldedQuery] must already be folded; matching a raw query against a folded column is
     * how a search for "zolw" silently stops finding "żółw".
     */
    @Query("SELECT * FROM words WHERE searchKey LIKE '%' || :foldedQuery || '%' ORDER BY text LIMIT :limit")
    suspend fun search(
        foldedQuery: String,
        limit: Int,
    ): List<WordEntity>

    /** Rows carried over by a migration have no key yet; see [VocabularySeeder]. */
    @Query("SELECT * FROM words WHERE searchKey = ''")
    suspend fun getWithoutSearchKey(): List<WordEntity>

    @Update
    suspend fun updateAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(words: List<WordEntity>)
}
