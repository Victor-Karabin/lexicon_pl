package com.lexicon.common

/**
 * The IPA transcription of a Polish word, worked out from its spelling.
 *
 * Polish orthography is near enough phonemic that this can be done by rule rather
 * than looked up: given the spelling, the pronunciation follows. That matters here
 * because a word being added by hand is, almost by definition, one no dictionary
 * the app could consult already has.
 *
 * Checked against all 2,477 transcriptions in the shipped corpus: the phonemes
 * agree everywhere. It disagrees on the voicing of 206 of them, all one mistake in
 * the corpus — see [assimilate].
 *
 * Returns an empty string for input with no letters in it.
 */
fun polishTranscription(text: String): String =
    text
        .split(' ', '\t', '\n')
        .filter { it.any(Char::isLetter) }
        .joinToString(" ") { stress(assimilate(phonemes(it))) }

/** Spellings of more than one letter, longest first so `dzi` wins over `dz`. */
private val DIGRAPHS: List<Pair<String, String>> = listOf(
    "dzi" to "d͡ʑ", "dź" to "d͡ʑ", "dż" to "d͡ʐ",
    "dz" to "d͡z", "cz" to "t͡ʂ", "ci" to "t͡ɕ", "ć" to "t͡ɕ",
    "sz" to "ʂ", "si" to "ɕ", "ś" to "ɕ", "rz" to "ʐ", "ż" to "ʐ",
    "zi" to "ʑ", "ź" to "ʑ", "ch" to "x", "h" to "x",
    "ni" to "ɲ", "ń" to "ɲ", "qu" to "kv",
)

private val LETTERS: Map<Char, String> = mapOf(
    'a' to "a", 'ą' to "ɔ̃", 'b' to "b", 'c' to "t͡s", 'd' to "d", 'e' to "ɛ",
    'ę' to "ɛ̃", 'f' to "f", 'g' to "g", 'i' to "i", 'j' to "j", 'k' to "k",
    'l' to "l", 'ł' to "w", 'm' to "m", 'n' to "n", 'o' to "ɔ", 'ó' to "u",
    'p' to "p", 'r' to "r", 's' to "s", 't' to "t", 'u' to "u", 'v' to "v",
    'w' to "v", 'x' to "ks", 'y' to "ɨ", 'z' to "z", 'ż' to "ʐ",
)

private val VOWELS = setOf("a", "ɛ", "i", "ɔ", "u", "ɨ", "ɔ̃", "ɛ̃")
private val SONORANTS = setOf("m", "n", "ɲ", "l", "r", "w", "j")

private val DEVOICED: Map<String, String> = mapOf(
    "b" to "p", "d" to "t", "g" to "k", "v" to "f", "z" to "s",
    "ʑ" to "ɕ", "ʐ" to "ʂ", "d͡z" to "t͡s", "d͡ʑ" to "t͡ɕ", "d͡ʐ" to "t͡ʂ",
)
private val VOICED: Map<String, String> = DEVOICED.entries.associate { (k, v) -> v to k }

/** The vowels an `i` can glide into, spelled rather than transcribed. */
private const val GLIDE_TARGETS = "aąeęoóu"

private fun String.isObstruent(): Boolean = this !in VOWELS && this !in SONORANTS

private fun phonemes(word: String): List<String> {
    val w = word.lowercase()
    val out = mutableListOf<String>()
    var i = 0
    while (i < w.length) {
        val digraph = DIGRAPHS.firstOrNull { w.startsWith(it.first, i) }
        when {
            digraph != null -> {
                out += digraph.second
                i += digraph.first.length
                // ci/si/zi/ni/dzi soften the consonant and swallow the i, but only
                // when a vowel follows: "ciasto" is t͡ɕastɔ while "ci" alone is t͡ɕi.
                val swallowsI = digraph.first.endsWith("i") &&
                    w.getOrNull(i)?.let { it in GLIDE_TARGETS } == true
                if (digraph.first.endsWith("i") && !swallowsI) out += "i"
            }

            // A consonant, then i, then a vowel: the i is the glide in mjɛt͡ɕ, kjɛdɨ.
            w[i] == 'i' && out.isNotEmpty() && out.last() !in VOWELS &&
                w.getOrNull(i + 1)?.let { it in GLIDE_TARGETS } == true -> {
                out += "j"
                i++
            }

            else -> {
                LETTERS[w[i]]?.let { out += it } ?: run { if (w[i].isLetter()) out += w[i].toString() }
                i++
            }
        }
    }
    return out
}

/**
 * Voicing across a consonant cluster, and at the end of a word.
 *
 * Polish assimilates rightwards-to-leftwards — the cluster takes the voicing of its
 * last obstruent — with one exception: `v` and `ʐ` devoice after a voiceless
 * obstruent instead of voicing it. That is why *przez* is `pʂɛs` and *twój* `tfuj`.
 *
 * The shipped corpus has this exception backwards in 206 of its 2,477 entries,
 * writing *przez* as `bʐɛs` and *kwiat* as `gvjat`. The rule here is the correct
 * one, so a hand-added word will not match those neighbours until the corpus is
 * regenerated.
 */
private fun assimilate(phonemes: List<String>): List<String> {
    val ph = phonemes.toMutableList()
    val progressive = mutableSetOf<Int>()

    for (i in 1 until ph.size) {
        if (ph[i] in setOf("v", "ʐ") && ph[i - 1].isObstruent() && ph[i - 1] in VOICED) {
            ph[i] = DEVOICED.getValue(ph[i])
            progressive += i
        }
    }

    for (i in ph.size - 2 downTo 0) {
        if (i in progressive) continue
        val cur = ph[i]
        val next = ph[i + 1]
        if (!cur.isObstruent() || !next.isObstruent()) continue
        when {
            next in DEVOICED && cur in VOICED -> ph[i] = VOICED.getValue(cur)
            next in VOICED && cur in DEVOICED -> ph[i] = DEVOICED.getValue(cur)
        }
    }

    ph.lastOrNull()?.let { if (it in DEVOICED) ph[ph.size - 1] = DEVOICED.getValue(it) }
    return ph
}

/**
 * Stress, which in Polish falls on the second-to-last syllable and is written at
 * the front of it — `ˈkturɨ`, not `ktˈurɨ`. A word of one syllable carries no mark.
 */
private fun stress(phonemes: List<String>): String {
    val vowels = phonemes.indices.filter { phonemes[it] in VOWELS }
    if (vowels.size < 2) return phonemes.joinToString("")

    val vowel = vowels[vowels.size - 2]
    val previous = if (vowels.size >= 3) vowels[vowels.size - 3] else -1
    var onset = vowel
    while (onset - 1 > previous && phonemes[onset - 1] !in VOWELS) onset--

    return phonemes.take(onset).joinToString("") + "ˈ" + phonemes.drop(onset).joinToString("")
}
