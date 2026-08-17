package com.lexicon.interactors.presets

interface DeleteWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}
