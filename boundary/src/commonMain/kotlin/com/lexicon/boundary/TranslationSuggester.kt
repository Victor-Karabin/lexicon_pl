package com.lexicon.boundary

interface TranslationSuggester {
    suspend fun suggest(
        text: String,
        direction: TranslationDirection,
        limit: Int,
    ): List<String>
}
