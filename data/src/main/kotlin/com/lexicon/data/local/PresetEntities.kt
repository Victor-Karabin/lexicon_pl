package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Presets as stored rows rather than a parsed asset.
 *
 * Localized titles are kept as the JSON object they arrive as, not split into one column per
 * language: the format allows any language tag, and a column per tag would need a schema change
 * for each new one.
 */
@Entity(tableName = "preset_categories")
data class PresetCategoryEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val titleJson: String,
)

@Entity(tableName = "presets", indices = [Index("categoryId")])
data class PresetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val titleJson: String,
    val descriptionJson: String,
    val icon: String?,
    val color: String?,
    val popularity: Int,
    val estimatedSeconds: Long,
)

/**
 * Preset membership. [position] preserves the order the asset gave, which is what makes
 * "100 most common words" arrive in frequency order rather than by id.
 */
@Entity(
    tableName = "preset_words",
    primaryKeys = ["presetId", "wordId"],
    indices = [Index("presetId"), Index("wordId")],
)
data class PresetWordEntity(
    val presetId: String,
    val wordId: Long,
    val position: Int,
)

/**
 * Presets the user has deleted.
 *
 * A separate table because the catalogue is replaced wholesale on every sync: a flag on the
 * preset row would be wiped by the next import and the preset would reappear.
 */
@Entity(tableName = "deleted_presets")
data class DeletedPresetEntity(
    @PrimaryKey val presetId: String,
)
