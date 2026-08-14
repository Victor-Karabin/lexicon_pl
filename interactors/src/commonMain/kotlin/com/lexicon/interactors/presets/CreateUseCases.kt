package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

/** What is wrong with a draft, in the order the form should complain about it. */
enum class WordDraftProblem {
    MISSING_TEXT,
    MISSING_TRANSLATION,

    /** The corpus, or an earlier addition, already has this Polish word. */
    ALREADY_EXISTS,
}

enum class PresetDraftProblem { MISSING_TITLE }

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

interface CreatePresetUseCase {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        icon: String? = null,
        color: String? = null,
        wordIds: List<VocabularyId> = emptyList(),
    ): Result<VocabularyPreset>
}

/** Raised by [CreateWordUseCase] so the form can point at the field at fault. */
class WordDraftException(val problem: WordDraftProblem) : Exception(problem.name)

class PresetDraftException(val problem: PresetDraftProblem) : Exception(problem.name)

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

/**
 * Pictures to choose between for a word. [skip] drops the ones already offered, so
 * asking for more turns up something new rather than the same batch again.
 */
interface SearchImageCandidatesUseCase {
    suspend operator fun invoke(
        query: String,
        skip: Int = 0,
    ): ImmutableList<String>
}
