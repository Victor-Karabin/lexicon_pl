package com.lexicon.interactors.presets

/**
 * Rewrites a word the learner edited.
 *
 * [presetIds] is the full set the word should end up in, not a set to add: the form
 * shows every preset with the current ones lit, so leaving one is as ordinary as
 * joining one. Only the difference is written.
 */
interface UpdateWordUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        text: String,
        translation: String,
        imageUrl: String? = null,
        presetIds: List<PresetId> = emptyList(),
    ): Result<PresetWord>
}
