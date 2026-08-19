package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId

interface RestoreWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}
