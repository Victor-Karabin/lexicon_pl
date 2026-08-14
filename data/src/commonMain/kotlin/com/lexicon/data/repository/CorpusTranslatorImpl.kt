package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator
import com.lexicon.common.foldForSearch
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao

/**
 * Answers from the words the app already ships, before anything goes over the wire.
 *
 * It only knows the 2,477 words in the corpus, which is exactly the set a learner is
 * least likely to be adding by hand — so this is not the main source, just the free
 * and instant one. It also settles the question the remote translator cannot: whether
 * the word is one the app already has.
 */
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

        // The search index folds both halves of a word into one key, so a hit here
        // may have matched on either side; the direction decides which half to read
        // back and which to compare against.
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
