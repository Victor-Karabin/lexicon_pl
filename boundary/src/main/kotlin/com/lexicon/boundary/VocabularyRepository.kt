package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    /**
     * Returns up to [count] distinct vocabulary items in random order, drawn from the study
     * set: the favourited words when there are any, and the whole vocabulary otherwise.
     *
     * The fallback is what makes favouriting safe to offer. Without it, a user who has
     * favourited nothing — which is everyone on first run — would have no words to train on.
     */
    suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary>

    /**
     * Returns the items carrying [ids], in no guaranteed order and skipping ids that no
     * longer exist — a preset built against an older corpus stays usable rather than failing.
     */
    suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary>

    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    /** Emits on every change, so a favourite toggled on one screen is reflected on the others. */
    fun observeFavouriteIds(): Flow<Set<Long>>
}
