package com.lexicon.interactors.presets

/** The word behind an id, for the form that edits it. */
interface GetWordUseCase {
    suspend operator fun invoke(id: VocabularyId): PresetWord?
}
