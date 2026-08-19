package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    suspend fun seedFromAsset(): SeedOutcomeBoundary

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

    suspend fun countStudyWords(excludePhrases: Boolean = false): Int

    suspend fun countWords(): Int

    suspend fun createWord(
        text: String,
        translation: String,
        transcription: String,
    ): VocabularyItemBoundary

    suspend fun updateWord(
        id: Long,
        text: String,
        translation: String,
        transcription: String,
    ): VocabularyItemBoundary

    suspend fun findWordByText(text: String): VocabularyItemBoundary?

    suspend fun getWord(id: Long): VocabularyItemBoundary?

    suspend fun deleteWord(id: Long)

    suspend fun restoreWord(id: Long)

    suspend fun setFavourite(
        ids: List<Long>,
        isFavourite: Boolean,
    )

    fun observeFavouriteIds(): Flow<Set<Long>>

    suspend fun allWordIds(): List<Long>

    suspend fun wordIdsForLevel(level: String): List<Long>

    suspend fun favouriteWordIds(): List<Long>
}
