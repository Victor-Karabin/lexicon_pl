package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator
import com.lexicon.common.foldForSearch
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao

class CorpusTranslatorImpl(
    private val wordDao: WordDao,
    private val vocabularySeeder: VocabularySeeder,
) : Translator {
    override suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String? {
        val needle = text.foldForSearch()
        if (needle.isEmpty()) return null
        vocabularySeeder.ensureSeeded()

        return wordDao
            .search(foldedQuery = needle, levels = emptyList(), ignoreLevels = 1, limit = CORPUS_MATCH_LIMIT)
            .firstNotNullOfOrNull { word ->
                when (direction) {
                    TranslationDirection.EN_TO_PL -> word.text.takeIf { word.translation.foldForSearch() == needle }
                    TranslationDirection.PL_TO_EN -> word.translation.takeIf { word.text.foldForSearch() == needle }
                }
            }?.takeIf { it.isNotBlank() }
    }
}

private const val CORPUS_MATCH_LIMIT = 25
