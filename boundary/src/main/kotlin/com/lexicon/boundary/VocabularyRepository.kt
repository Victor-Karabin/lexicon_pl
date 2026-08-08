package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    suspend fun syncFromSource(): SyncOutcomeBoundary

    suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary>

    suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary>

    suspend fun search(
        foldedQuery: String,
        levels: Set<String>,
        limit: Int,
    ): List<VocabularyItemBoundary>

    suspend fun countStudyWords(): Int

    suspend fun countWords(): Int

    suspend fun deleteWord(id: Long)

    suspend fun restoreWord(id: Long)

    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    fun observeFavouriteIds(): Flow<Set<Long>>
}
