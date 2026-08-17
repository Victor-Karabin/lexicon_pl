package com.lexicon.interactors.presets

interface RestoreWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}
