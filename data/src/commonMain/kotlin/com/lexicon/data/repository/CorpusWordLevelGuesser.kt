package com.lexicon.data.repository

import com.lexicon.boundary.WordLevelGuesser
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao
import com.lexicon.model.vocabulary.CefrLevel

class CorpusWordLevelGuesser(
    private val wordDao: WordDao,
    private val vocabularySeeder: VocabularySeeder,
) : WordLevelGuesser {
    override suspend fun guess(
        text: String,
        translation: String,
    ): CefrLevel? {
        val meaning = translation.asSenseKey()
        if (meaning.isEmpty()) return null
        vocabularySeeder.ensureSeeded()

        return wordDao
            .withTranslationContaining(
                meaning,
                CORPUS_LOOKUP_LIMIT,
            ).filter { it.translation.hasSense(meaning) }
            .mapNotNull { CefrLevel.ofName(it.cefr.ifEmpty { null }) }
            .minByOrNull { it.ordinal }
    }
}

class MergingWordLevelGuesser(
    private val guessers: List<WordLevelGuesser>,
) : WordLevelGuesser {
    override suspend fun guess(
        text: String,
        translation: String,
    ): CefrLevel? =
        guessers.firstNotNullOfOrNull { guesser ->
            runCatching { guesser.guess(text, translation) }.getOrNull()
        }
}
