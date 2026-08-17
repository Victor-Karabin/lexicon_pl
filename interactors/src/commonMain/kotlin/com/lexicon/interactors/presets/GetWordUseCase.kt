package com.lexicon.interactors.presets

interface GetWordUseCase {
    suspend operator fun invoke(id: VocabularyId): PresetWord?
}
