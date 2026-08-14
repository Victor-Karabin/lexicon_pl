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

/**
 * The category every hand-made preset is filed under.
 *
 * It is not in the catalogue asset, so re-seeding would drop it along with the
 * shipped categories; [PresetDao.replaceCatalog] puts it back. The negative order
 * sorts the learner's own presets above the shipped ones, which are ordered by
 * category and then popularity.
 */
const val USER_PRESET_CATEGORY_ID = "my-presets"
private const val USER_PRESET_CATEGORY_ORDER = -1

/** Both languages, the way the catalogue asset carries them for every other category. */
val USER_PRESET_CATEGORY_TITLE = mapOf("en" to "My presets", "pl" to "Moje zestawy")

/**
 * Ids for hand-made presets, kept in their own `my-` namespace so one can never
 * take the id of a preset the catalogue ships now or later — the two live in the
 * same table, and a clash would have the asset silently overwrite the learner's.
 *
 * [suffix] disambiguates two presets named the same thing; the caller counts up
 * until it finds an id nothing holds.
 */
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
    /**
     * Made by the learner rather than shipped in the catalogue asset. Re-seeding
     * clears and rewrites the shipped presets wholesale, so [PresetDao.replaceCatalog]
     * has to carry these across by hand or they would vanish on the next update
     * that touches vocabulary_presets.json.
     */
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

/**
 * A membership the learner changed by hand, kept apart from [PresetWordEntity]
 * because re-seeding the catalogue clears and rewrites that table wholesale.
 * Recording the intent separately — like [DeletedPresetEntity] does for presets —
 * is what lets an edit survive the next time the shipped catalogue changes.
 */
@Entity(
    tableName = "preset_word_overrides",
    primaryKeys = ["presetId", "wordId"],
    indices = [Index("presetId"), Index("wordId")],
)
data class PresetWordOverrideEntity(
    val presetId: String,
    val wordId: Long,
    /** True if the learner added the word to the preset, false if they took it out. */
    val isMember: Boolean,
)
