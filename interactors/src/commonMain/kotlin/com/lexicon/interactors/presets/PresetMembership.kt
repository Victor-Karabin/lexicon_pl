package com.lexicon.interactors.presets

/** A preset, and whether the word in question is currently in it. */
data class PresetMembership(
    val preset: VocabularyPreset,
    val isMember: Boolean,
)
