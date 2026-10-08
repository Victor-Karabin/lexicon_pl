"""Reads the frequency lists of the balanced Corpus of Contemporary Polish (KWJP).

The lists are published by the Institute of Computer Science of the Polish Academy of
Sciences at https://github.com/ipipan/kwjp100-varia under CC BY 4.0. Put these files
from its freqlists directory into tools/vocabulary/.cache/; they are not committed:

    kwjp100-slowa-lemma-all.csv.gz          lemma, part of speech, frequencies
    kwjp100-2grams-orth_lc-all.csv.gz       two-word sequences, lowercased
    kwjp100-3grams-orth_lc-all.csv.gz       three-word sequences, lowercased

Frequencies are ARF, the average reduced frequency: a word used often but only in a few
texts counts for less than one spread evenly, which suits a learner who meets words
across many kinds of text rather than in one book about football.
"""

from __future__ import annotations

import csv
import gzip
import re
from collections import defaultdict

import sgjp

LEMMAS = "kwjp100-slowa-lemma-all.csv.gz"
NGRAMS = {2: "kwjp100-2grams-orth_lc-all.csv.gz", 3: "kwjp100-3grams-orth_lc-all.csv.gz"}

WORD = re.compile(r"^[a-ząćęłńóśźż]+(?:-[a-ząćęłńóśźż]+)*$")

READ_AHEAD = 3

SKIPPED_TAGS = {"interp", "brev", "ign", "xxx", "num", "romandig", "burk", "dig"}


def lemma_frequency() -> dict[str, float]:
    arf: dict[str, float] = defaultdict(float)
    with gzip.open(sgjp.CACHE / LEMMAS, "rt", encoding="utf-8") as lines:
        rows = csv.reader(lines)
        next(rows)
        for cols in rows:
            if WORD.match(cols[0]) and cols[1] not in SKIPPED_TAGS:
                arf[cols[0]] += float(cols[4])
    return dict(arf)


def ngram_frequency(length: int, wanted: set[tuple[str, ...]] | None = None, top: int = 0) -> dict[tuple[str, ...], float]:
    """The lists come sorted by raw frequency, so the top ones by ARF lie near the start."""
    found: dict[tuple[str, ...], float] = {}
    with gzip.open(sgjp.CACHE / NGRAMS[length], "rt", encoding="utf-8") as lines:
        rows = csv.reader(lines)
        next(rows)
        for cols in rows:
            words = tuple(cols[:length])
            if not all(WORD.match(word) for word in words):
                continue
            if wanted is None or words in wanted:
                found[words] = found.get(words, 0.0) + float(cols[length + 2])
            if top and wanted is None and len(found) >= top * READ_AHEAD:
                break
    if top:
        found = dict(sorted(found.items(), key=lambda item: -item[1])[:top])
    return found
