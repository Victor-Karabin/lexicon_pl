package com.lexicon.boundary

/**
 * Where presets come from. Deliberately says nothing about *which* source: the bundled
 * asset today, and downloaded packs, user-created or community presets later, are all
 * implementations of this one contract rather than new contracts of their own.
 */
interface VocabularyPresetRepository {
    /** Brings the stored catalogue in line with the bundled source, reporting what changed. */
    suspend fun syncFromSource(): SyncOutcomeBoundary

    suspend fun getPresets(): List<VocabularyPresetBoundary>

    /** Returns null when no preset carries [id], which callers must handle. */
    suspend fun getPreset(id: String): VocabularyPresetBoundary?

    suspend fun getCategories(): List<PresetCategoryBoundary>
}
