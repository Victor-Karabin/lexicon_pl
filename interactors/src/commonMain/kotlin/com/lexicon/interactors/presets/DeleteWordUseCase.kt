package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId

interface DeleteWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}
