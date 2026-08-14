package com.lexicon.boundary

/** The direction a translation runs in; the app only ever needs these two. */
enum class TranslationDirection { EN_TO_PL, PL_TO_EN }

interface Translator {
    /**
     * The other half of a word, or null when nothing could be found — no match, no
     * network, no API key configured. A caller filling a field in as the learner
     * types treats null as "leave it alone" rather than as an error worth reporting.
     */
    suspend fun translate(
        text: String,
        direction: TranslationDirection,
    ): String?
}
