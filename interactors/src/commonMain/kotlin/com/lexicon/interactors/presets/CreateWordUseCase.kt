package com.lexicon.interactors.presets

/**
 * A word the learner wrote, together with where it should be filed and what it
 * should look like in the picture trainings.
 *
 * The pronunciation is not asked for: Polish spelling says how a word is said, so
 * it is worked out from [text] once the word exists rather than typed.
 *
 * [imageUrl] is pinned against the English translation rather than stored on the
 * word: that is the key Puzzle, Image Test and Memory Cards look an image up by, so
 * pinning it there is what makes the chosen picture actually appear in them.
 */
interface CreateWordUseCase {
    suspend operator fun invoke(
        text: String,
        translation: String,
        imageUrl: String? = null,
        presetIds: List<PresetId> = emptyList(),
    ): Result<PresetWord>
}
