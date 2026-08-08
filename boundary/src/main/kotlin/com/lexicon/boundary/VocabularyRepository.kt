package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    /**
     * Brings the store in line with the bundled source, reporting what changed. Called once at
     * startup rather than lazily, so nothing later has to wonder whether the words are current.
     */
    suspend fun syncFromSource(): SyncOutcomeBoundary

    /**
     * Returns up to [count] distinct vocabulary items in random order, drawn from the study
     * set — the favourited words, and only those.
     *
     * A user who has favourited nothing therefore has nothing to train on, which is why every
     * training is fronted by a readiness check rather than left to start an empty session.
     */
    suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary>

    /**
     * Returns the items carrying [ids], in no guaranteed order and skipping ids that no
     * longer exist — a preset built against an older corpus stays usable rather than failing.
     */
    suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary>

    /**
     * Words whose Polish or English side matches [foldedQuery], which the caller must already
     * have folded with the same rule the stored keys use, narrowed to [levels] when any are
     * given. An empty [levels] means every level rather than none. Ordered alphabetically.
     */
    suspend fun search(
        foldedQuery: String,
        levels: Set<String>,
        limit: Int,
    ): List<VocabularyItemBoundary>

    /**
     * Size of the study set. This is what a training has to work with, so it is what "not
     * enough words" is measured against.
     */
    suspend fun countStudyWords(): Int

    /** Every word held, favourited or not — how "is there anything to work with" is answered. */
    suspend fun countWords(): Int

    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    /** Emits on every change, so a favourite toggled on one screen is reflected on the others. */
    fun observeFavouriteIds(): Flow<Set<Long>>
}
