package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId

fun interface FillWordGrammarUseCase {
    suspend operator fun invoke(id: VocabularyId)
}
