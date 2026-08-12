package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    suspend fun syncFromSource(): SyncOutcomeBoundary

    /**
     * Random words to build a session from. [restrictToIds] narrows the draw to a
     * subset — a course lesson's own words — and an empty list means the whole
     * study set.
     */
    suspend fun getRandomItems(
        count: Int,
        restrictToIds: List<Long> = emptyList(),
    ): List<VocabularyItemBoundary>

    suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary>

    suspend fun search(
        foldedQuery: String,
        levels: Set<String>,
        limit: Int,
    ): List<VocabularyItemBoundary>

    /** [excludePhrases] narrows the count to single words — what Crossword can place. */
    suspend fun countStudyWords(excludePhrases: Boolean = false): Int

    suspend fun countWords(): Int

    suspend fun deleteWord(id: Long)

    suspend fun restoreWord(id: Long)

    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    fun observeFavouriteIds(): Flow<Set<Long>>
}
