package com.lexicon.interactors.presets

interface DeleteWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}

interface RestoreWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}

interface DeletePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}

interface RestorePresetUseCase {
    suspend operator fun invoke(id: PresetId)
}
