"""Polish grapheme-to-phoneme conversion.

Polish orthography is close to phonemic, so transcriptions are derived from spelling
rather than typed by hand: 2000 hand-written IPA strings would be 2000 chances to slip,
and a rule that is wrong is wrong visibly and in one place.

Covers the rules that matter for citation forms of single words: digraphs, softening
before <i>, word-final devoicing, regressive voicing assimilation within a word, and
penultimate stress. It does not attempt sandhi across word boundaries, or the lexical
exceptions (mostly loanwords) where spelling and pronunciation genuinely diverge.
"""

from __future__ import annotations

# Longest-first: <dzi> must be tried before <dz>, <ch> before <c>.
DIGRAPHS = [
    ("dzi", "d͡ʑi"),
    ("ch", "x"),
    ("cz", "t͡ʂ"),
    ("sz", "ʂ"),
    ("rz", "ʐ"),
    ("dź", "d͡ʑ"),
    ("dż", "d͡ʐ"),
    ("dz", "d͡z"),
    ("ci", "t͡ɕi"),
    ("si", "ɕi"),
    ("zi", "ʑi"),
    ("ni", "ɲi"),
]

SINGLES = {
    "a": "a",
    "ą": "ɔ̃",
    "b": "b",
    "c": "t͡s",
    "ć": "t͡ɕ",
    "d": "d",
    "e": "ɛ",
    "ę": "ɛ̃",
    "f": "f",
    "g": "g",
    "h": "x",
    "i": "i",
    "j": "j",
    "k": "k",
    "l": "l",
    "ł": "w",
    "m": "m",
    "n": "n",
    "ń": "ɲ",
    "o": "ɔ",
    "ó": "u",
    "p": "p",
    "r": "r",
    "s": "s",
    "ś": "ɕ",
    "t": "t",
    "u": "u",
    "w": "v",
    "y": "ɨ",
    "z": "z",
    "ź": "ʑ",
    "ż": "ʐ",
}

# Obstruent pairs, used for final devoicing and assimilation.
DEVOICE = {
    "b": "p",
    "d": "t",
    "g": "k",
    "v": "f",
    "z": "s",
    "ʑ": "ɕ",
    "ʐ": "ʂ",
    "d͡z": "t͡s",
    "d͡ʑ": "t͡ɕ",
    "d͡ʐ": "t͡ʂ",
}
VOICE = {v: k for k, v in DEVOICE.items()}

VOWELS = set("aɛɔiuɨ") | {"ɔ̃", "ɛ̃"}
SONORANTS = set("mnr lɲjw".replace(" ", ""))


def _to_phonemes(word: str) -> list[str]:
    """Splits spelling into phoneme symbols, before any contextual adjustment."""
    out: list[str] = []
    i = 0
    lowered = word.lower()
    while i < len(lowered):
        for graph, phon in DIGRAPHS:
            if lowered.startswith(graph, i):
                # <si>/<ci>/<zi>/<ni> before a vowel are a soft consonant only: the <i>
                # is a softness marker, not a vowel of its own (siano, not si-ano).
                if graph in ("ci", "si", "zi", "ni", "dzi"):
                    nxt = lowered[i + len(graph):i + len(graph) + 1]
                    if nxt and nxt in "aeąęoóuy":
                        out.append(phon[:-1])
                        i += len(graph)
                        break
                out.extend([phon[:-1], "i"] if phon.endswith("i") and len(phon) > 1 else [phon])
                i += len(graph)
                break
        else:
            ch = lowered[i]
            # <i> before another vowel is a glide, not a syllable: pies is [pjɛs], not
            # [ˈpiɛs]. It still palatalises the preceding consonant, which this
            # transcription leaves implicit rather than marking on every one.
            if ch == "i" and lowered[i + 1:i + 2] in ("a", "e", "ą", "ę", "o", "ó", "u"):
                out.append("j")
            elif ch in SINGLES:
                out.append(SINGLES[ch])
            i += 1
    return out


def _is_voiced_obstruent(p: str) -> bool:
    return p in DEVOICE


def _is_voiceless_obstruent(p: str) -> bool:
    return p in VOICE


def _assimilate(phonemes: list[str]) -> list[str]:
    """Regressive voicing assimilation, then word-final devoicing.

    Polish obstruent clusters take their voicing from the last obstruent, so <książka>
    is [kɕɔ̃ʂka] and <prośba> is [ˈprɔʑba]. Sonorants do not trigger it.
    """
    out = list(phonemes)
    for i in range(len(out) - 2, -1, -1):
        nxt = out[i + 1]
        if _is_voiceless_obstruent(nxt) and _is_voiced_obstruent(out[i]):
            out[i] = DEVOICE[out[i]]
        elif _is_voiced_obstruent(nxt) and _is_voiceless_obstruent(out[i]):
            # <w> and <rz> are the usual exception: they devoice after a voiceless
            # consonant rather than voicing it, which the loop above already handled.
            out[i] = VOICE[out[i]]
    if out and _is_voiced_obstruent(out[-1]):
        out[-1] = DEVOICE[out[-1]]
    return out


def _stress(phonemes: list[str]) -> str:
    """Polish stress is fixed on the penultimate syllable; one-syllable words are unmarked."""
    vowel_positions = [i for i, p in enumerate(phonemes) if p in VOWELS]
    if len(vowel_positions) < 2:
        return "".join(phonemes)
    target = vowel_positions[-2]
    # The mark goes before the whole onset of the stressed syllable, not before its vowel.
    start = target
    while start > 0 and phonemes[start - 1] not in VOWELS:
        start -= 1
    return "".join(phonemes[:start]) + "ˈ" + "".join(phonemes[start:])


def transcribe(word: str) -> str:
    """IPA for a single Polish word or phrase, without surrounding brackets."""
    if " " in word.strip():
        return " ".join(transcribe(part) for part in word.split())
    return _stress(_assimilate(_to_phonemes(word)))
