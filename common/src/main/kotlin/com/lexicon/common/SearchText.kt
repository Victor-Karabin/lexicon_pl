package com.lexicon.common

/**
 * Folds text to the form searches compare against: lower case, Polish diacritics stripped.
 *
 * Stripping the diacritics is the point. Someone learning Polish is usually typing on a
 * keyboard that has no ą or ż, and a search for "zolw" that cannot find "żółw" fails exactly
 * when it is needed. It also has to be the *same* folding everywhere: a stored key folded one
 * way and a query folded another simply never match.
 */
private val POLISH_DIACRITICS = mapOf(
    'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
    'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
)

fun String.foldForSearch(): String = lowercase().map { POLISH_DIACRITICS[it] ?: it }.joinToString("").trim()
