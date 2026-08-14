package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface VocabularyPresetRepository {
    suspend fun syncFromSource(): SyncOutcomeBoundary

    suspend fun getPresets(): List<VocabularyPresetBoundary>

    fun observePresets(): Flow<List<VocabularyPresetBoundary>>

    suspend fun getPreset(id: String): VocabularyPresetBoundary?

    suspend fun getCategories(): List<PresetCategoryBoundary>

    /**
     * Stores a preset of the learner's own, filed under its own category so it sorts
     * above the shipped ones, and returns it.
     */
    suspend fun createPreset(
        title: Map<String, String>,
        description: Map<String, String>,
        icon: String?,
        color: String?,
        wordIds: List<Long>,
    ): VocabularyPresetBoundary

    suspend fun deletePreset(id: String)

    suspend fun restorePreset(id: String)

    /** The presets a word currently belongs to; a word can be in any number of them. */
    suspend fun getPresetIdsForWord(wordId: Long): List<String>

    suspend fun setWordInPreset(
        presetId: String,
        wordId: Long,
        isMember: Boolean,
    )
}
