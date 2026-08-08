package com.lexicon.boundary

interface VocabularyRepository {
    /** Returns up to [count] distinct vocabulary items in random order. */
    suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary>

    /**
     * Returns the items carrying [ids], in no guaranteed order and skipping ids that no
     * longer exist — a preset built against an older corpus stays usable rather than failing.
     */
    suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary>
}
