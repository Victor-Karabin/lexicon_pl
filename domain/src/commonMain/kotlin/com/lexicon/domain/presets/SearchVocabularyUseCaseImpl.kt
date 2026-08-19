package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.foldForSearch
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class SearchVocabularyUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : SearchVocabularyUseCase {
    override suspend fun invoke(
        query: String,
        levels: Set<CefrLevel>,
        limit: Int,
    ): ImmutableList<Word> {
        val folded = query.foldForSearch()
        if (folded.isEmpty() && levels.isEmpty()) return persistentListOf()

        return vocabularyRepository
            .search(folded, levels.mapTo(mutableSetOf()) { it.name }, limit)
            .map { it }
            .toImmutableList()
    }
}
