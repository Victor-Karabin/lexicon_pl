package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lexicon.common.foldForSearch

@Entity(tableName = "preset_categories")
data class PresetCategoryEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val titleJson: String,
)

const val USER_PRESET_CATEGORY_ID = "my-presets"
private const val USER_PRESET_CATEGORY_ORDER = -1

val USER_PRESET_CATEGORY_TITLE = mapOf("en" to "My presets", "pl" to "Moje zestawy")

internal fun userPresetId(
    title: String,
    suffix: Int = 0,
): String {
    val slug = title
        .foldForSearch()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .trim('-')
        .replace(Regex("-+"), "-")
        .take(USER_PRESET_SLUG_MAX)
        .ifEmpty { "preset" }
    return if (suffix == 0) "my-$slug" else "my-$slug-$suffix"
}

private const val USER_PRESET_SLUG_MAX = 40

internal fun userPresetCategory(title: Map<String, String> = USER_PRESET_CATEGORY_TITLE): PresetCategoryEntity =
    PresetCategoryEntity(
        id = USER_PRESET_CATEGORY_ID,
        sortOrder = USER_PRESET_CATEGORY_ORDER,
        titleJson = title.encodeLocalized(),
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
    val isUserCreated: Boolean = false,
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

@Entity(
    tableName = "preset_word_overrides",
    primaryKeys = ["presetId", "wordId"],
    indices = [Index("presetId"), Index("wordId")],
)
data class PresetWordOverrideEntity(
    val presetId: String,
    val wordId: Long,
    val isMember: Boolean,
)
