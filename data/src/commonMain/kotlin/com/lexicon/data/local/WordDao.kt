package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE isFavourite = 1 AND isDeleted = 0 ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomForStudy(count: Int): List<WordEntity>

    /**
     * A session over a fixed subset — a course lesson. Unlike [getRandomForStudy] this
     * ignores the favourite flag: choosing the lesson is itself the choice of what to
     * study, so it would be surprising to have to heart every word first.
     */
    @Query("SELECT * FROM words WHERE id IN (:ids) AND isDeleted = 0 ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomFromIds(
        ids: List<Long>,
        count: Int,
    ): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids) AND isDeleted = 0")
    suspend fun getByIds(ids: List<Long>): List<WordEntity>

    @Query("UPDATE words SET isFavourite = :isFavourite WHERE id IN (:ids)")
    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    @Query("SELECT id FROM words WHERE isFavourite = 1 AND isDeleted = 0")
    fun observeFavouriteIds(): Flow<List<Long>>

    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE isFavourite = 1 AND isDeleted = 0
          AND (:excludePhrases = 0 OR text NOT LIKE '% %')
        """,
    )
    suspend fun countForStudy(excludePhrases: Int): Int

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

    @Query("SELECT COUNT(*) FROM words")
    suspend fun countIncludingDeleted(): Int

    @Query("UPDATE words SET isDeleted = :isDeleted WHERE id = :id")
    suspend fun setDeleted(
        id: Long,
        isDeleted: Boolean,
    )

    @Insert
    suspend fun insertAll(words: List<WordEntity>)

    @Insert
    suspend fun insert(word: WordEntity)

    /** Lowest id in the table, which [nextUserWordId] counts down from. */
    @Query("SELECT MIN(id) FROM words")
    suspend fun lowestId(): Long?

    /** An existing word with the same Polish text, so the same word is not added twice. */
    @Query("SELECT * FROM words WHERE text = :text COLLATE NOCASE LIMIT 1")
    suspend fun findByText(text: String): WordEntity?

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun findById(id: Long): WordEntity?

    /**
     * Ids a program's vocabulary scope can draw on, in corpus order.
     *
     * Ordered by id rather than alphabetically because the corpus is numbered by
     * frequency: the lower the id, the more often the word turns up, which is the
     * order a beginner should meet them in.
     */
    @Query("SELECT id FROM words WHERE isDeleted = 0 ORDER BY id")
    suspend fun allWordIds(): List<Long>

    @Query("SELECT id FROM words WHERE isDeleted = 0 AND cefr = :level ORDER BY id")
    suspend fun wordIdsForLevel(level: String): List<Long>

    @Query("SELECT id FROM words WHERE isDeleted = 0 AND isFavourite = 1 ORDER BY id")
    suspend fun favouriteWordIds(): List<Long>

    /**
     * Rewrites a word the learner edited, and marks it theirs.
     *
     * A shipped word becomes user-created the moment it is edited: [VocabularySeeder]
     * rewrites every word it still considers the asset's from the asset, so without
     * this the edit would last only until the next catalogue update.
     */
    @Query(
        """
        UPDATE words
        SET text = :text,
            translation = :translation,
            transcription = :transcription,
            searchKey = :searchKey,
            isUserCreated = 1
        WHERE id = :id
        """,
    )
    suspend fun updateWord(
        id: Long,
        text: String,
        translation: String,
        transcription: String,
        searchKey: String,
    )

    /**
     * Applies a seed-asset diff as one write: an interrupted app process can no
     * longer leave the added/removed/changed rows partially committed.
     */
    @Transaction
    suspend fun reconcile(
        added: List<WordEntity>,
        removedIds: List<Long>,
        changed: List<WordEntity>,
    ) {
        insertAll(added)
        if (removedIds.isNotEmpty()) deleteByIds(removedIds)
        if (changed.isNotEmpty()) updateAll(changed)
    }
}
