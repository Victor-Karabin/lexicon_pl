package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyPresetRepository {
    suspend fun syncFromSource(): SyncOutcomeBoundary

    suspend fun getPresets(): List<VocabularyPresetBoundary>

    fun observePresets(): Flow<List<VocabularyPresetBoundary>>

    suspend fun getPreset(id: String): VocabularyPresetBoundary?

    suspend fun getCategories(): List<PresetCategoryBoundary>

    suspend fun deletePreset(id: String)

    suspend fun restorePreset(id: String)
}
