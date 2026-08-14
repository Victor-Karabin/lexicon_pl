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

    /**
     * Stores a word of the learner's own and returns it. Its id is allocated below
     * the asset's range so it cannot collide with a word a later release ships.
     */
    suspend fun createWord(
        text: String,
        translation: String,
        transcription: String,
    ): VocabularyItemBoundary

    /** An existing word with the same Polish text, so the same one is not added twice. */
    suspend fun findWordByText(text: String): VocabularyItemBoundary?

    suspend fun deleteWord(id: Long)

    suspend fun restoreWord(id: Long)

    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    fun observeFavouriteIds(): Flow<Set<Long>>
}
