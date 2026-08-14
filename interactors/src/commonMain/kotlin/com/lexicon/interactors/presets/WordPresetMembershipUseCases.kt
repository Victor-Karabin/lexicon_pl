package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

/** A preset, and whether the word in question is currently in it. */
data class PresetMembership(
    val preset: VocabularyPreset,
    val isMember: Boolean,
)

/**
 * Every preset the learner could file a word under, flagged with the ones it is
 * already in. A word can be in any number of presets at once, so this is a
 * checklist rather than a single choice.
 */
interface GetWordPresetMembershipsUseCase {
    suspend operator fun invoke(wordId: VocabularyId): ImmutableList<PresetMembership>
}

interface SetWordPresetMembershipUseCase {
    suspend operator fun invoke(
        presetId: PresetId,
        wordId: VocabularyId,
        isMember: Boolean,
    )
}
