package com.lexicon.data.remote.translate

private const val WORD_GROWTH_ALLOWED = 2

fun looksLikeATranslation(
    source: String,
    candidate: String,
): Boolean {
    if (candidate.isBlank()) return false

    if (candidate.equals(source.trim(), ignoreCase = true)) return false

    if (candidate.wordCount() > source.wordCount() + WORD_GROWTH_ALLOWED) return false

    if (candidate.any { it in ":;\"" }) return false
    return true
}

private fun String.wordCount(): Int = trim().split(' ', '\t').count { it.isNotBlank() }
