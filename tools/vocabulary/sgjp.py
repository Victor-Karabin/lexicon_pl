"""Reads the Grammatical Dictionary of Polish (SGJP) that Morfeusz 2 ships as text.

The file is sgjp-YYYYMMDD.tab.gz from https://morfeusz.sgjp.pl/download/, released
under the two-clause BSD licence. Put it in tools/vocabulary/.cache/; it is not
committed. Every line is one inflected form:

    form <TAB> lemma[:homonym] <TAB> tag <TAB> name class <TAB> qualifiers

Tags follow the NKJP tagset, for example subst:sg:gen.acc:m2 or fin:pl:ter:imperf.
Dot-joined values list every slot the form fills.
"""

from __future__ import annotations

import gzip
import re
from dataclasses import dataclass, field
from pathlib import Path

from build_examples import TOOLS

CACHE = TOOLS / ".cache"

GENDERS = {
    "m1": "masculine personal",
    "m2": "masculine animate",
    "m3": "masculine inanimate",
    "f": "feminine",
    "n": "neuter",
}

CASES = {
    "nominative": "nom",
    "genitive": "gen",
    "dative": "dat",
    "accusative": "acc",
    "instrumental": "inst",
    "locative": "loc",
    "vocative": "voc",
}

PERSONS = {
    "ja": ("sg", "pri"),
    "ty": ("sg", "sec"),
    "on/ona/ono": ("sg", "ter"),
    "my": ("pl", "pri"),
    "wy": ("pl", "sec"),
    "oni/one": ("pl", "ter"),
}

PART_OF_SPEECH = {
    "subst": "n",
    "depr": "n",
    "adj": "adj",
    "adja": "adj",
    "adjp": "adj",
    "adjc": "adj",
    "adv": "adv",
    "prep": "prep",
    "conj": "conj",
    "comp": "conj",
    "ppron12": "prn",
    "ppron3": "prn",
    "siebie": "prn",
    "num": "num",
    "numcol": "num",
    "qub": "part",
    "interj": "interj",
    "pred": "v",
}

VULGAR_ROOT = re.compile(r"jeb|pierd|kurw|cwel|pedal|chuj|pizd")

VERB_TAGS = {"fin", "inf", "praet", "impt", "imps", "ger", "pact", "ppas", "pcon", "pant", "bedzie", "winien"}


@dataclass
class Form:
    text: str
    tag: list[list[str]]
    qualifiers: str

    @property
    def standard(self) -> bool:
        return not self.qualifiers


@dataclass
class Lexeme:
    lemma: str
    homonym: str
    forms: list[Form] = field(default_factory=list)

    @property
    def part_of_speech(self) -> str:
        heads = {form.tag[0][0] for form in self.forms}
        if heads & VERB_TAGS:
            return "v"
        for head in heads:
            if head in PART_OF_SPEECH:
                return PART_OF_SPEECH[head]
        return next(iter(heads), "")

    @property
    def qualifiers(self) -> str:
        return next((form.qualifiers for form in self.forms if form.qualifiers), "")

    def slot(self, *wanted: str, head: str | None = None) -> list[Form]:
        found = []
        for form in self.forms:
            if head and form.tag[0][0] != head:
                continue
            values = form.tag[1:]
            if all(any(value in part for part in values) for value in wanted):
                found.append(form)
        return found

    def texts(self, *wanted: str, head: str | None = None) -> list[str]:
        forms = self.slot(*wanted, head=head)
        preferred = [form.text for form in forms if form.standard] or [form.text for form in forms]
        return list(dict.fromkeys(preferred))

    def all_texts(self, *wanted: str, head: str | None = None) -> set[str]:
        return {form.text for form in self.slot(*wanted, head=head)}

    @property
    def gender(self) -> str | None:
        nouns = [form for form in self.forms if form.tag[0][0] == "subst"]
        if any("pt" in part for form in nouns for part in form.tag[1:]):
            return "plural only"
        for form in nouns:
            if len(form.tag) > 3:
                for value in form.tag[3]:
                    if value in GENDERS:
                        return GENDERS[value]
        return None

    @property
    def aspect(self) -> str | None:
        for form in self.forms:
            if form.tag[0][0] == "inf" and len(form.tag) > 1:
                return {"perf": "perfective", "imperf": "imperfective"}.get(form.tag[1][0])
        return None


def dictionary_file(name: str = "sgjp") -> Path:
    found = sorted(CACHE.glob(f"{name}-*.tab.gz"))
    if not found:
        raise FileNotFoundError(f"put {name}-YYYYMMDD.tab.gz from https://morfeusz.sgjp.pl/download/ into {CACHE}")
    return found[-1]


def load(lemmas: set[str], name: str = "sgjp") -> dict[str, list[Lexeme]]:
    lexemes: dict[tuple[str, str], Lexeme] = {}
    with gzip.open(dictionary_file(name), "rt", encoding="utf-8") as lines:
        for line in lines:
            if line.startswith("#") or "\t" not in line:
                continue
            cols = line.rstrip("\n").split("\t")
            if len(cols) < 3:
                continue
            lemma, _, homonym = cols[1].partition(":")
            if lemma not in lemmas:
                continue
            key = (lemma, homonym)
            lexeme = lexemes.setdefault(key, Lexeme(lemma=lemma, homonym=homonym))
            lexeme.forms.append(
                Form(
                    text=cols[0],
                    tag=[part.split(".") for part in cols[2].split(":")],
                    qualifiers=cols[4] if len(cols) > 4 else "",
                )
            )
    grouped: dict[str, list[Lexeme]] = {}
    for lexeme in lexemes.values():
        grouped.setdefault(lexeme.lemma, []).append(lexeme)
    return grouped


def best(candidates: list[Lexeme], part_of_speech: str, gender: str | None = None) -> Lexeme | None:
    fitting = [lexeme for lexeme in candidates if lexeme.part_of_speech == part_of_speech]
    if not fitting:
        return None
    return min(
        fitting,
        key=lambda lexeme: (
            bool(lexeme.qualifiers),
            gender is not None and lexeme.gender != gender,
            -len(lexeme.forms),
        ),
    )


def declension(lexeme: Lexeme) -> dict[str, tuple[list[str], list[str]]]:
    return {
        name: (lexeme.texts("sg", tag, head="subst"), lexeme.texts("pl", tag, head="subst"))
        for name, tag in CASES.items()
    }


def adjective_forms(lexeme: Lexeme) -> dict[str, list[str]]:
    return {
        "masculine": lexeme.texts("sg", "nom", "m1", "pos", head="adj"),
        "feminine": lexeme.texts("sg", "nom", "f", "pos", head="adj"),
        "neuter": lexeme.texts("sg", "nom", "n", "pos", head="adj"),
        "pluralPersonal": lexeme.texts("pl", "nom", "m1", "pos", head="adj"),
        "pluralOther": lexeme.texts("pl", "nom", "f", "pos", head="adj"),
    }


def present_tense(lexeme: Lexeme) -> dict[str, list[str]]:
    return {person: lexeme.texts(number, who, head="fin") for person, (number, who) in PERSONS.items()}


def split_cases(declension: str) -> dict[str, tuple[str, str]]:
    cases = {}
    for name, cell in zip(CASES, declension.split(";")):
        singular, _, plural = cell.partition("|")
        cases[name] = (singular.strip(), plural.strip())
    return cases


def variants(cell: str) -> list[str]:
    usable = re.sub(r"\bN/A\b", "", cell, flags=re.IGNORECASE)
    parts = [part.strip() for part in usable.replace(";", "/").split("/")]
    return [part for part in parts if part]


def verb_lemma(infinitive: str) -> tuple[str, str, str]:
    words = infinitive.split(" ")
    at = next((index for index, word in enumerate(words) if word.endswith(("ć", "c"))), 0)
    before = "".join(word + " " for word in words[:at])
    after = "".join(" " + word for word in words[at + 1 :])
    return words[at], before, after


def noun_mismatches(lexeme: Lexeme, ours: dict[str, tuple[str, str]]) -> list[str]:
    preferred = declension(lexeme)
    wrong = []
    for case, (singular, plural) in ours.items():
        for number, mine, shown in (("sg", singular, preferred[case][0]), ("pl", plural, preferred[case][1])):
            allowed = lexeme.all_texts(number, CASES[case], head="subst")
            if mine and allowed and mine not in allowed:
                wrong.append(f"{case} {number}: {mine} → {'/'.join(shown) or '/'.join(sorted(allowed))}")
    return wrong


def closest(candidates: list[Lexeme], part_of_speech: str, mismatches) -> Lexeme | None:
    fitting = [lexeme for lexeme in candidates if lexeme.part_of_speech == part_of_speech]
    if not fitting:
        return None
    fallback = best(fitting, part_of_speech)
    return min(fitting, key=lambda lexeme: (len(mismatches(lexeme)), lexeme is not fallback))
