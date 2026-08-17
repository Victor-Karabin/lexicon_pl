package com.lexicon.interactors.presets

/**
 * The picture a word already has, if any.
 *
 * Answers from the cache first, so this is the one the trainings show — which is
 * what makes it the right one to mark as chosen when the word is opened for editing.
 */
interface GetPinnedImageUseCase {
    suspend operator fun invoke(translation: String): String?
}
