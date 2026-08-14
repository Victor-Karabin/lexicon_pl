package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM preset_categories ORDER BY sortOrder")
    suspend fun getCategories(): List<PresetCategoryEntity>

    @Query("SELECT * FROM presets")
    suspend fun getPresets(): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPreset(id: String): PresetEntity?

    @Query(
        """
        SELECT pw.wordId FROM preset_words pw
        INNER JOIN words w ON w.id = pw.wordId
        WHERE pw.presetId = :presetId AND w.isDeleted = 0
        ORDER BY pw.position
        """,
    )
    suspend fun getWordIds(presetId: String): List<Long>

    @Query(
        """
        SELECT pw.* FROM preset_words pw
        INNER JOIN words w ON w.id = pw.wordId
        INNER JOIN presets p ON p.id = pw.presetId
        WHERE w.isDeleted = 0
        ORDER BY pw.presetId, pw.position
        """,
    )
    fun observeMemberships(): Flow<List<PresetWordEntity>>

    @Query(
        """
        SELECT pw.* FROM preset_words pw
        INNER JOIN words w ON w.id = pw.wordId
        WHERE w.isDeleted = 0
        ORDER BY pw.presetId, pw.position
        """,
    )
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
