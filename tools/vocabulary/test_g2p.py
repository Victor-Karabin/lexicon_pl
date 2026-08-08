#!/usr/bin/env python3
"""Regression tests for the Polish transcriber:  python3 tools/vocabulary/test_g2p.py

Cases are real words whose IPA is not in dispute, chosen so each exercises one rule. If a
rule changes, the case that pinned it is the one that fails, which is the point of picking
them this way rather than sampling the corpus.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from g2p import transcribe  # noqa: E402

CASES = {
    # Plain consonants and vowels.
    "kot": "kɔt",
    "dom": "dɔm",
    "woda": "ˈvɔda",
    "ryba": "ˈrɨba",
    # Digraphs.
    "czas": "t͡ʂas",
    "szkoła": "ˈʂkɔwa",
    "chleb": "xlɛp",
    "życie": "ˈʐɨt͡ɕɛ",
    "dziecko": "ˈd͡ʑɛt͡skɔ",
    "dzień": "d͡ʑɛɲ",
    # Nasal vowels, and <ó>/<ł> spelling one sound each.
    "ręka": "ˈrɛ̃ka",
    "stół": "stuw",
    # <i> as a softness marker before a vowel, not a syllable of its own.
    "pies": "pjɛs",
    "miasto": "ˈmjastɔ",
    "ziemia": "ˈʑɛmja",
    "niebo": "ˈɲɛbɔ",
    # Word-final devoicing.
    "chleb": "xlɛp",
    "samochód": "saˈmɔxut",
    # Regressive voicing assimilation, in both directions.
    "książka": "ˈkɕɔ̃ʂka",
    "wszystko": "ˈfʂɨstkɔ",
    "prośba": "ˈprɔʑba",
    # Stress on the penultimate syllable, marked before the whole onset.
    "jabłko": "ˈjabwkɔ",
    "biblioteka": "bibljɔˈtɛka",
    # Multi-word entries are transcribed word by word.
    "dzień dobry": "d͡ʑɛɲ ˈdɔbrɨ",
}


def main() -> int:
    failures = [(word, transcribe(word), expected) for word, expected in CASES.items() if transcribe(word) != expected]
    for word, got, expected in failures:
        print(f"FAIL {word}: got {got}, expected {expected}")
    print(f"{len(CASES) - len(failures)}/{len(CASES)} passed")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
