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

    @Query(
        """
        SELECT pw.presetId FROM preset_words pw
        INNER JOIN presets p ON p.id = pw.presetId
        WHERE pw.wordId = :wordId
        """,
    )
    suspend fun getPresetIdsForWord(wordId: Long): List<String>

    /** Appends to the end of the preset, so a hand-added word lands last rather than mid-list. */
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM preset_words WHERE presetId = :presetId")
    suspend fun nextPosition(presetId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: PresetWordEntity)

    @Query("DELETE FROM preset_words WHERE presetId = :presetId AND wordId = :wordId")
    suspend fun deleteMembership(
        presetId: String,
        wordId: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: PresetWordOverrideEntity)

    @Query("SELECT * FROM preset_word_overrides")
    suspend fun getOverrides(): List<PresetWordOverrideEntity>

    /** Puts one word into (or out of) a preset, remembering the choice across re-seeds. */
    @Transaction
    suspend fun setWordInPreset(
        presetId: String,
        wordId: Long,
        isMember: Boolean,
    ) {
        if (isMember) {
            insertMembership(PresetWordEntity(presetId = presetId, wordId = wordId, position = nextPosition(presetId)))
        } else {
            deleteMembership(presetId, wordId)
        }
        upsertOverride(PresetWordOverrideEntity(presetId = presetId, wordId = wordId, isMember = isMember))
    }

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
        reapplyOverrides(presets.mapTo(mutableSetOf()) { it.id })
    }

    /**
     * Replays the learner's own membership edits over the freshly-seeded catalogue,
     * which the insert above has just reset to what the asset ships.
     *
     * An override for a preset the catalogue no longer has is dropped: the preset is
     * gone, so the edit has nothing left to apply to.
     */
    private suspend fun reapplyOverrides(livePresetIds: Set<String>) {
        for (override in getOverrides()) {
            if (override.presetId !in livePresetIds) continue
            if (override.isMember) {
                insertMembership(
                    PresetWordEntity(
                        presetId = override.presetId,
                        wordId = override.wordId,
                        position = nextPosition(override.presetId),
                    ),
                )
            } else {
                deleteMembership(override.presetId, override.wordId)
            }
        }
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
