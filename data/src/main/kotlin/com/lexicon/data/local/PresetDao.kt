package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PresetDao {
    @Query("SELECT * FROM preset_categories ORDER BY sortOrder")
    suspend fun getCategories(): List<PresetCategoryEntity>

    @Query("SELECT * FROM presets")
    suspend fun getPresets(): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPreset(id: String): PresetEntity?

    @Query("SELECT wordId FROM preset_words WHERE presetId = :presetId ORDER BY position")
    suspend fun getWordIds(presetId: String): List<Long>

    /** One query for every membership, so listing presets is not one query per preset. */
    @Query("SELECT * FROM preset_words ORDER BY presetId, position")
    suspend fun getAllMemberships(): List<PresetWordEntity>

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun countPresets(): Int

    @Query("SELECT presetId FROM deleted_presets")
    suspend fun getDeletedPresetIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedPreset(deleted: DeletedPresetEntity)

    @Query("DELETE FROM deleted_presets WHERE presetId = :presetId")
    suspend fun undeletePreset(presetId: String)

    @Query("DELETE FROM presets WHERE id = :presetId")
    suspend fun deletePreset(presetId: String)

    @Query("DELETE FROM preset_words WHERE presetId = :presetId")
    suspend fun deleteMemberships(presetId: String)

    /**
     * Replaces the whole catalogue in one transaction. Presets are reference data — nothing the
     * user owns lives on these rows — so replacing is both correct and simpler than reconciling
     * row by row, and a half-written catalogue can never be observed.
     */
    @Transaction
    suspend fun replaceCatalog(
        categories: List<PresetCategoryEntity>,
        presets: List<PresetEntity>,
        memberships: List<PresetWordEntity>,
    ) {
        clearMemberships()
        clearPresets()
        clearCategories()
        insertCategories(categories)
        insertPresets(presets)
        insertMemberships(memberships)
    }

    @Query("DELETE FROM preset_words")
    suspend fun clearMemberships()

    @Query("DELETE FROM presets")
    suspend fun clearPresets()

    @Query("DELETE FROM preset_categories")
    suspend fun clearCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<PresetCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberships(memberships: List<PresetWordEntity>)
}
