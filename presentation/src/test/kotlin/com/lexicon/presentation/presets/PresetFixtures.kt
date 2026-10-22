package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.PresetCategory
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration.Companion.seconds

internal fun presetOf(id: String) =
    VocabularyPreset(
        id = PresetId(id),
        title = LocalizedText(mapOf("en" to id)),
        description = LocalizedText(mapOf("en" to id)),
        category = PresetCategory(id = "cat", order = 0, title = LocalizedText(mapOf("en" to "Category"))),
        icon = null,
        color = null,
        popularity = 0,
        estimatedDuration = 0.seconds,
        vocabularyIds = persistentListOf(),
    )

internal fun membershipsOf(vararg pairs: Pair<String, Boolean>): ImmutableList<PresetMembership> =
    pairs.map { (id, isMember) -> PresetMembership(presetOf(id), isMember) }.toImmutableList()
