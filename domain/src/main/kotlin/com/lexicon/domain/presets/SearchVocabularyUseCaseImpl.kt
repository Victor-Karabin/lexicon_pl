package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.foldForSearch
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

class SearchVocabularyUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
    ) : SearchVocabularyUseCase {
        override suspend fun invoke(
            query: String,
            limit: Int,
        ): ImmutableList<PresetWord> {
            // An empty query means "nothing typed yet", not "every word": dumping 1,700 rows
            // into the list would bury the search box that is about to be used.
            val folded = query.foldForSearch()
            if (folded.isEmpty()) return persistentListOf()

            return vocabularyRepository.search(folded, limit).map { it.toPresetWord() }.toImmutableList()
        }
    }
