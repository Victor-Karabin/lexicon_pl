package com.lexicon.application.conjugation

import com.lexicon.interactors.conjugation.VerbConjugation

internal const val MIN_STEM_LENGTH = 2

internal const val MIN_ENDING_LENGTH = 1

data class ConjugationSplit(
    val stem: String,
    val endings: List<String>,
)

/**
 * Splits a verb's forms into a shared stem and the endings that differ.
 *
 * The stem is the longest prefix every usable form shares, taken from the data rather
 * than from a table of suffixes: `chodzić` yields `chodz` + `ę/isz/i/imy/icie/ą`, while
 * `być` shares almost nothing across `jestem` and `są` and yields no usable split at all.
 * Callers fall back to whole forms when this returns null, which is what keeps irregular
 * verbs working instead of being carved up into endings that do not exist.
 */
internal fun VerbConjugation.split(): ConjugationSplit? {
    val forms = persons.mapNotNull { formsFor(it).firstOrNull() }.filter { it.isNotBlank() }
    if (forms.size < 2) return null

    val stem = forms.reduce(::commonPrefix)
    if (stem.length < MIN_STEM_LENGTH) return null

    val endings = forms.map { it.removePrefix(stem) }
    if (endings.any { it.length < MIN_ENDING_LENGTH }) return null

    // A stem ending mid-gap leaves endings like " się", which is the reflexive particle
    // rather than an ending; those verbs are better asked as whole forms.
    if (endings.any { it.first().isWhitespace() }) return null
    if (endings.distinct().size < 2) return null

    return ConjugationSplit(stem = stem, endings = endings)
}

/** The ending of one form against the verb's shared stem, or null when it has none. */
internal fun VerbConjugation.endingFor(form: String): String? {
    val split = split() ?: return null
    return form.removePrefix(split.stem).takeIf { it != form && it.isNotEmpty() }
}

private fun commonPrefix(
    left: String,
    right: String,
): String = left.commonPrefixWith(right)
