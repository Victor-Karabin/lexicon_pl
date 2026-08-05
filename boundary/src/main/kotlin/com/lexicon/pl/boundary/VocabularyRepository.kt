package com.lexicon.pl.boundary

interface VocabularyRepository {
    /** Returns up to [count] distinct vocabulary items in random order. */
    suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary>
}
