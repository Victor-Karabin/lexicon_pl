package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyPresetRepository {
    suspend fun seedFromAsset(): SeedOutcomeBoundary

    suspend fun getPresets(): List<VocabularyPresetBoundary>

    fun observePresets(): Flow<List<VocabularyPresetBoundary>>

    suspend fun getPreset(id: String): VocabularyPresetBoundary?

    suspend fun getCategories(): List<PresetCategoryBoundary>

    suspend fun createPreset(
        title: Map<String, String>,
        description: Map<String, String>,
        icon: String?,
        color: String?,
        wordIds: List<Long>,
    ): VocabularyPresetBoundary

    suspend fun deletePreset(id: String)

    suspend fun restorePreset(id: String)

    suspend fun getPresetIdsForWord(wordId: Long): List<String>

    suspend fun setWordInPreset(
        presetId: String,
        wordId: Long,
        isMember: Boolean,
    )
}
