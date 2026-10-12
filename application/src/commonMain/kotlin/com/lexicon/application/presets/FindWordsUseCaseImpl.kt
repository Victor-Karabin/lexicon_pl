package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.FindWordsUseCase
import com.lexicon.model.vocabulary.Word

private val trailing = charArrayOf('?', '!', '.', '…', ' ')
private val endings = listOf("", "?", "!", ".", "…", "…?", "…!")

class FindWordsUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : FindWordsUseCase {
    override suspend fun invoke(texts: List<String>): Map<String, Word> {
        val wanted = texts.distinct().associateWith { it.bare() }.filterValues { it.isNotEmpty() }
        val words = vocabularyRepository.wordsWithTexts(wanted.values.flatMap { it.spellings() }.distinct())
        return wanted.mapNotNull { (text, bare) ->
            words.firstOrNull { it.text.bare().equals(bare, ignoreCase = true) }?.let { text to it }
        }.toMap()
    }

    private fun String.bare(): String = trim().trimEnd(*trailing)

    private fun String.spellings(): List<String> =
        listOf(this, replaceFirstChar { it.lowercase() }, replaceFirstChar { it.uppercase() })
            .distinct()
            .flatMap { base -> endings.map { base + it } }
}
