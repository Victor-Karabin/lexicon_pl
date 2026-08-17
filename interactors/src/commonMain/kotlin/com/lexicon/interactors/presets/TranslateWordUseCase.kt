package com.lexicon.interactors.presets

/**
 * Fills in the other half of a word as the learner types. Returns null rather than
 * failing when nothing is found: the field is theirs to type over, and an empty
 * suggestion is not an error worth interrupting them for.
 */
interface TranslateWordUseCase {
    suspend operator fun invoke(
        text: String,
        toPolish: Boolean,
    ): String?
}
