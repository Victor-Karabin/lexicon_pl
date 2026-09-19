package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.TranslationSuggester
import com.lexicon.common.foldForSearch
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao

internal const val CORPUS_LOOKUP_LIMIT = 500

private val SENSE_SEPARATORS = Regex("[,;/]")

internal fun senses(translation: String): List<String> =
    translation
        .split(SENSE_SEPARATORS)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

internal fun String.asSenseKey(): String = foldForSearch().removePrefix("to ").trim()

internal fun String.hasSense(key: String): Boolean = senses(this).any { it.asSenseKey() == key }

class CorpusTranslationSuggester(
    private val wordDao: WordDao,
    private val vocabularySeeder: VocabularySeeder,
) : TranslationSuggester {
    override suspend fun suggest(
        text: String,
        direction: TranslationDirection,
        limit: Int,
    ): List<String> {
        val needle = text.asSenseKey()
        if (needle.isEmpty()) return emptyList()
        vocabularySeeder.ensureSeeded()

        val candidates = when (direction) {
            TranslationDirection.EN_TO_PL -> wordDao.withTranslationContaining(needle, CORPUS_LOOKUP_LIMIT)
            TranslationDirection.PL_TO_EN -> wordDao.withTextStarting(text.foldForSearch(), CORPUS_LOOKUP_LIMIT)
        }

        return candidates
            .flatMap { word ->
                when (direction) {
                    TranslationDirection.EN_TO_PL -> listOfNotNull(word.text.takeIf { word.translation.hasSense(needle) })
                    TranslationDirection.PL_TO_EN -> if (word.text.asSenseKey() == needle) senses(word.translation) else emptyList()
                }
            }.distinctBy { it.lowercase() }
            .take(limit)
    }
}

class MergingTranslationSuggester(
    private val suggesters: List<TranslationSuggester>,
) : TranslationSuggester {
    override suspend fun suggest(
        text: String,
        direction: TranslationDirection,
        limit: Int,
    ): List<String> {
        if (text.isBlank()) return emptyList()

        val found = mutableListOf<String>()
        for (suggester in suggesters) {
            val missing = limit - found.size
            if (missing <= 0) break

            val more = runCatching { suggester.suggest(text, direction, missing) }.getOrDefault(emptyList())
            found += more.filterNot { candidate -> found.any { it.equals(candidate, ignoreCase = true) } }
        }
        return found.take(limit)
    }
}
