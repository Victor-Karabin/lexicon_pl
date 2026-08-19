package com.lexicon.application.conjugation

import com.lexicon.interactors.conjugation.VerbConjugation

internal const val MIN_STEM_LENGTH = 2

internal const val MIN_ENDING_LENGTH = 1

data class ConjugationSplit(
    val stem: String,
    val endings: List<String>,
)

internal fun VerbConjugation.split(): ConjugationSplit? {
    val forms = persons.mapNotNull { formsFor(it).firstOrNull() }.filter { it.isNotBlank() }
    if (forms.size < 2) return null

    val stem = forms.reduce(::commonPrefix)
    if (stem.length < MIN_STEM_LENGTH) return null

    val endings = forms.map { it.removePrefix(stem) }
    if (endings.any { it.length < MIN_ENDING_LENGTH }) return null

    if (endings.any { it.first().isWhitespace() }) return null
    if (endings.distinct().size < 2) return null

    return ConjugationSplit(stem = stem, endings = endings)
}

internal fun VerbConjugation.endingFor(form: String): String? {
    val split = split() ?: return null
    return form.removePrefix(split.stem).takeIf { it != form && it.isNotEmpty() }
}

private fun commonPrefix(
    left: String,
    right: String,
): String = left.commonPrefixWith(right)
