package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

@Entity(tableName = "deleted_presets")
data class DeletedPresetEntity(
    @PrimaryKey val presetId: String,
)
