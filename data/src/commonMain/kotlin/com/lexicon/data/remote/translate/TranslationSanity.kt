package com.lexicon.data.remote.translate

/** How much longer than the original an answer may be before it is clearly not one. */
private const val WORD_GROWTH_ALLOWED = 2

/**
 * Whether an answer from a translation memory is a translation at all.
 *
 * MyMemory answers with the closest segment somebody has already translated, and
 * when it has no good match it returns something unrelated — asked for *smok* it
 * offered "Iain Walker wrote:". This is what keeps answers like that out of the
 * field, on both platforms: the rule has to be the same one, or a word filled in on
 * Android would be rejected on iOS.
 */
fun looksLikeATranslation(
    source: String,
    candidate: String,
): Boolean {
    if (candidate.isBlank()) return false
    // The same text back means it found nothing and echoed the query.
    if (candidate.equals(source.trim(), ignoreCase = true)) return false
    // A sentence in reply to one word is a stray segment out of the memory, not a
    // translation of it.
    if (candidate.wordCount() > source.wordCount() + WORD_GROWTH_ALLOWED) return false
    // Punctuation that belongs to running prose rather than to a dictionary entry.
    if (candidate.any { it in ":;\"" }) return false
    return true
}

private fun String.wordCount(): Int = trim().split(' ', '\t').count { it.isNotBlank() }
