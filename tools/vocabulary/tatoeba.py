"""Reads the Polish sentences of Tatoeba with their English translations, lemmatised by SGJP.

Tatoeba (https://tatoeba.org) is a collection of sentences written and translated by
volunteers, released under CC BY 2.0 FR. Put these exports from
https://downloads.tatoeba.org/exports/per_language/ into tools/vocabulary/.cache/;
they are not committed:

    pol/pol_sentences_detailed.tsv.bz2      id, lang, text, author, added, modified
    pol/pol-eng_links.tsv.bz2               Polish id, English id
    eng/eng_sentences_detailed.tsv.bz2      the English side

Every sentence used in the app keeps its id and author, so it can be credited.
"""

from __future__ import annotations

import bz2
import gzip
import re
from collections import Counter, defaultdict
from dataclasses import dataclass, field

import sgjp

TOKEN = re.compile(r"[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż]+(?:-[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż]+)*")

CONDITIONAL = re.compile(r"^(?:że|a|gdy|jak|to|czy)?by(?:m|ś|śmy|ście)?$")

SKIPPED_TAGS = {"brev", "burk", "interp", "xxx", "ign"}

COMMON_NOUN_CLASSES = {"", "nazwa_pospolita"}


@dataclass
class Pair:
    id: str
    polish: str
    english: str
    author: str
    english_id: str
    english_author: str
    lemmas: list[str | None] = field(default_factory=list)

    @property
    def tokens(self) -> list[str]:
        return TOKEN.findall(self.polish)

    def marked(self, lemma: str) -> str | None:
        for token, found in zip(self.tokens, self.lemmas):
            if found == lemma:
                return re.sub(rf"(?<![\wąćęłńóśźż-]){re.escape(token)}(?![\wąćęłńóśźż-])", f"**{token}**", self.polish, count=1)
        return None


def words_of(sentence: str) -> list[str | None]:
    return [None if k > 0 and token[0].isupper() else token.lower() for k, token in enumerate(TOKEN.findall(sentence))]


def pairs() -> list[Pair]:
    polish = {}
    with bz2.open(sgjp.CACHE / "pol_sentences_detailed.tsv.bz2", "rt", encoding="utf-8") as lines:
        for line in lines:
            cols = line.rstrip("\n").split("\t")
            if len(cols) >= 4:
                polish[cols[0]] = (cols[2], cols[3])
    links: dict[str, str] = {}
    with bz2.open(sgjp.CACHE / "pol-eng_links.tsv.bz2", "rt", encoding="utf-8") as lines:
        for line in lines:
            polish_id, english_id = line.split()
            links.setdefault(polish_id, english_id)
    wanted = set(links.values())
    english = {}
    with bz2.open(sgjp.CACHE / "eng_sentences_detailed.tsv.bz2", "rt", encoding="utf-8") as lines:
        for line in lines:
            cols = line.rstrip("\n").split("\t")
            if cols[0] in wanted and len(cols) >= 4:
                english[cols[0]] = (cols[2], cols[3])
    return [
        Pair(id=i, polish=text, author=author, english=english[links[i]][0], english_id=links[i], english_author=english[links[i]][1])
        for i, (text, author) in polish.items()
        if i in links and links[i] in english
    ]


def polish_sentences() -> list[str]:
    with bz2.open(sgjp.CACHE / "pol_sentences_detailed.tsv.bz2", "rt", encoding="utf-8") as lines:
        return [cols[2] for cols in (line.rstrip("\n").split("\t") for line in lines) if len(cols) >= 3]


def recurrence(texts: list[str], longest: int) -> Counter:
    counts: Counter = Counter()
    for text in texts:
        words = [token.lower() for token in TOKEN.findall(text)]
        counts.update({tuple(words[i : i + n]) for n in range(2, longest + 1) for i in range(len(words) - n + 1)})
    return counts


def names(texts: list[str]) -> set[str]:
    capitalised: Counter = Counter()
    lowercase: Counter = Counter()
    for text in texts:
        for index, token in enumerate(TOKEN.findall(text)):
            if index and token[0].isupper():
                capitalised[token.lower()] += 1
            elif token[0].islower():
                lowercase[token] += 1
    return {word for word, count in capitalised.items() if count >= 3 and count > lowercase[word]}


class Lemmatiser:
    def __init__(self, sentences: list[Pair]):
        forms = Counter(word for pair in sentences for word in set(words_of(pair.polish)) if word)
        self.candidates: dict[str, set[str]] = defaultdict(set)
        self.vulgar: set[str] = set()
        with gzip.open(sgjp.dictionary_file(), "rt", encoding="utf-8") as lines:
            for line in lines:
                cols = line.rstrip("\n").split("\t")
                if len(cols) < 4:
                    continue
                form = cols[0].lower()
                if form not in forms:
                    continue
                lemma = cols[1].split(":")[0]
                qualifiers = cols[4] if len(cols) > 4 else ""
                if "wulg" in qualifiers:
                    self.vulgar.add(lemma)
                if cols[3] in COMMON_NOUN_CLASSES and lemma.islower() and cols[2].split(":")[0] not in SKIPPED_TAGS:
                    self.candidates[form].add(lemma)
        spread = Counter()
        for form, count in forms.items():
            for lemma in self.candidates.get(form, ()):
                spread[lemma] += count
        self.lemma_of = {
            form: max(lemmas, key=lambda lemma: (spread[lemma], lemma == form)) for form, lemmas in self.candidates.items()
        }

    def annotate(self, sentences: list[Pair]) -> None:
        for pair in sentences:
            pair.lemmas = [self.lemma_of.get(word) if word else None for word in words_of(pair.polish)]

    def is_known(self, word: str) -> bool:
        return word.lower() in self.candidates or bool(CONDITIONAL.match(word.lower()))

    def is_clean(self, pair: Pair) -> bool:
        return all(self.is_known(word) for word in words_of(pair.polish) if word)


def frequency(sentences: list[Pair]) -> Counter:
    counts: Counter = Counter()
    for pair in sentences:
        counts.update({lemma for lemma in pair.lemmas if lemma})
    return counts
