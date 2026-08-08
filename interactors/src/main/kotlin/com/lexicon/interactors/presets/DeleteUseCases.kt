package com.lexicon.interactors.presets

/**
 * Removes a word from the user's vocabulary for good — not from the current list, and not just
 * from the study set, which is what the heart is for.
 *
 * The deletion outlives a catalogue update: the bundled source still carries the word, so
 * without recording the decision the next sync would put it back.
 */
interface DeleteWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}

interface RestoreWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}

/** Removes a preset. The words it listed stay; only the collection goes. */
interface DeletePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}

interface RestorePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
