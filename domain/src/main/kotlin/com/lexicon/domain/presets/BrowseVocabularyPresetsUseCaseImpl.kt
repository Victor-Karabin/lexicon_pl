package com.lexicon.domain.presets

import com.lexicon.common.foldForSearch
import com.lexicon.interactors.presets.BrowsePresetsRequest
import com.lexicon.interactors.presets.BrowseVocabularyPresetsUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.PresetSort
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 * Search, filter and sort over the whole catalogue.
 *
 * Held in one use case because the three interact: filters narrow what search runs over,
 * and sorting has to apply to the result of both. Splitting them would let a caller apply
 * them in the wrong order and get a subtly different list.
 */
class BrowseVocabularyPresetsUseCaseImpl
    @Inject
    constructor(
        private val getPresets: GetVocabularyPresetsUseCase,
    ) : BrowseVocabularyPresetsUseCase {
        override suspend fun invoke(request: BrowsePresetsRequest): ImmutableList<VocabularyPreset> {
            val filtered = getPresets()
                .filter { request.categoryIds.isEmpty() || it.category.id in request.categoryIds }
                .filter { matches(it, request) }

            return filtered.sortedWith(comparator(request)).toImmutableList()
        }

        /**
         * Matches title, description and category name, so "food" finds the Food preset and
         * "everyday" finds everything in that category. Case- and accent-insensitive, because
         * a learner searching Polish titles cannot be expected to type ą and ż to find them.
         */
        private fun matches(
            preset: VocabularyPreset,
            request: BrowsePresetsRequest,
        ): Boolean {
            val query = request.query.foldForSearch()
            if (query.isEmpty()) return true
            val haystack = listOf(
                preset.title.resolve(request.languageTag),
                preset.description.resolve(request.languageTag),
                preset.category.title.resolve(request.languageTag),
            )
            return haystack.any { query in it.foldForSearch() }
        }

        private fun comparator(request: BrowsePresetsRequest): Comparator<VocabularyPreset> =
            when (request.sort) {
                PresetSort.POPULARITY -> compareBy { it.popularity }
                PresetSort.ALPHABETICAL -> compareBy { it.title.resolve(request.languageTag).foldForSearch() }
                PresetSort.WORD_COUNT_ASCENDING -> compareBy { it.wordCount }
                PresetSort.WORD_COUNT_DESCENDING -> compareByDescending { it.wordCount }
            }
    }
