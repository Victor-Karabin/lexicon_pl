package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyPreset

data class PresetMembership(
    val preset: VocabularyPreset,
    val isMember: Boolean,
)
