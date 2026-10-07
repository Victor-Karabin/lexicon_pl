#!/usr/bin/env python3
"""Checks the grammar the app ships against the Grammatical Dictionary of Polish (SGJP).

Run from anywhere:  python3 tools/vocabulary/compare_sgjp.py [report.md]

Reads
    tools/vocabulary/.cache/sgjp-*.tab.gz   see sgjp.py
    tools/vocabulary/corpus/**/*.tsv        the words and their part of speech
    tools/vocabulary/grammar.tsv            noun gender and declension, adjective forms
    data/src/androidMain/assets/conjugations.json

Writes a Markdown report: how many words SGJP knows, and every gender, case,
adjective form and verb form where the shipped data disagrees with it.
"""

from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

import sgjp
from build_examples import CONJUGATIONS, TOOLS, corpus_entries, read_tsv

PERSONS = list(sgjp.PERSONS)


def shipped_grammar() -> dict[str, list[str]]:
    return {cols[0]: cols for cols in read_tsv(TOOLS / "grammar.tsv") if len(cols) >= 3}


class Report:
    def __init__(self) -> None:
        self.counts: Counter[str] = Counter()
        self.sections: dict[str, list[str]] = {}

    def add(self, section: str, line: str) -> None:
        self.sections.setdefault(section, []).append(line)

    def count(self, key: str, amount: int = 1) -> None:
        self.counts[key] += amount


def compare_nouns(words: list[dict], grammar: dict, lexicon: dict, report: Report) -> None:
    for entry in words:
        row = grammar.get(entry["word"])
        ours_gender = row[2] if row and len(row) > 2 else ""
        ours = sgjp.split_cases(row[3]) if row and len(row) > 3 and row[3] else {}
        lexeme = sgjp.closest(
            lexicon.get(entry["word"], []),
            "n",
            lambda candidate: sgjp.noun_mismatches(candidate, ours) + ([ours_gender] if candidate.gender != ours_gender else []),
        )
        if lexeme is None:
            continue
        report.count("nouns checked")
        if ours_gender and lexeme.gender and ours_gender != lexeme.gender:
            report.count("noun gender wrong")
            report.add("Noun gender", f"| {entry['word']} | {entry['gloss']} | {ours_gender} | {lexeme.gender} |")
        if not ours:
            report.count("noun declension missing")
            continue
        report.count("noun forms checked", sum(bool(sg) + bool(pl) for sg, pl in ours.values()))
        wrong = sgjp.noun_mismatches(lexeme, ours)
        if wrong:
            report.count("noun forms wrong", len(wrong))
            report.count("nouns with a wrong form")
            report.add("Noun declension", f"| {entry['word']} | {entry['gloss']} | {'; '.join(wrong)} |")


def compare_adjectives(words: list[dict], grammar: dict, lexicon: dict, report: Report) -> None:
    fields = ["masculine", "feminine", "neuter", "pluralPersonal", "pluralOther"]
    for entry in words:
        row = grammar.get(entry["word"])
        lexeme = sgjp.best(lexicon.get(entry["word"], []), "adj")
        if lexeme is None:
            continue
        report.count("adjectives checked")
        if not row or len(row) < 9 or not row[4]:
            report.count("adjective forms missing")
            continue
        theirs = sgjp.adjective_forms(lexeme)
        wrong = []
        for name, mine in zip(fields, row[4:9]):
            report.count("adjective forms checked")
            if mine and theirs[name] and not set(sgjp.variants(mine)) <= set(theirs[name]):
                wrong.append(f"{name}: {mine} → {'/'.join(theirs[name])}")
        if wrong:
            report.count("adjective forms wrong", len(wrong))
            report.add("Adjective forms", f"| {entry['word']} | {entry['gloss']} | {'; '.join(wrong)} |")


def compare_verbs(conjugations: list[dict], lexicon: dict, report: Report) -> None:
    for verb in conjugations:
        infinitive = verb.get("bezokolicznik", "")
        lemma, reflexive = sgjp.verb_lemma(infinitive)
        mine_by_person = {person: sgjp.variants(verb.get(person) or "") for person in PERSONS}
        lexeme = sgjp.closest(
            lexicon.get(lemma, []),
            "v",
            lambda candidate: [
                person
                for person, forms in sgjp.present_tense(candidate).items()
                if mine_by_person[person] and not {form + reflexive for form in forms} >= set(mine_by_person[person])
            ],
        )
        if lexeme is None:
            report.count("verbs SGJP lacks")
            report.add("Verbs SGJP does not know", f"| {infinitive} | {verb.get('translation', '')} |")
            continue
        report.count("verbs checked")
        report.count(f"verbs {lexeme.aspect or 'without aspect'}")
        theirs = sgjp.present_tense(lexeme)
        wrong = []
        for person in PERSONS:
            mine = sgjp.variants(verb.get(person) or "")
            allowed = {form + reflexive for form in theirs[person]}
            if not mine:
                report.count("verb forms missing" if allowed else "verb forms SGJP lacks too")
                continue
            report.count("verb forms checked")
            if allowed and not set(mine) <= allowed:
                wrong.append(f"{person}: {' / '.join(mine)} → {'/'.join(sorted(allowed))}")
        if wrong:
            report.count("verb forms wrong", len(wrong))
            report.count("verbs with a wrong form")
            report.add("Verb conjugation", f"| {infinitive} | {lexeme.aspect or ''} | {'; '.join(wrong)} |")


def compare_coverage(words: list[dict], lexicon: dict, report: Report) -> None:
    for entry in words:
        if entry["pos"] == "expr" or " " in entry["word"]:
            report.count("phrases (not in SGJP)")
            continue
        lemma = sgjp.verb_lemma(entry["word"])[0] if entry["pos"] == "v" else entry["word"]
        candidates = lexicon.get(lemma, [])
        if not candidates:
            report.count("words SGJP lacks")
            report.add("Words SGJP does not know", f"| {entry['word']} | {entry['gloss']} | {entry['pos']} |")
            continue
        report.count("words SGJP knows")
        theirs = {lexeme.part_of_speech for lexeme in candidates}
        if entry["pos"] not in theirs:
            report.count("part of speech differs")
            report.add(
                "Part of speech",
                f"| {entry['word']} | {entry['gloss']} | {entry['pos']} | {', '.join(sorted(theirs))} |",
            )


HEADERS = {
    "Part of speech": "| word | meaning | app | SGJP |\n|---|---|---|---|",
    "Noun gender": "| word | meaning | app | SGJP |\n|---|---|---|---|",
    "Noun declension": "| word | meaning | app form → SGJP |\n|---|---|---|",
    "Adjective forms": "| word | meaning | app form → SGJP |\n|---|---|---|",
    "Verb conjugation": "| verb | aspect | app form → SGJP |\n|---|---|---|",
    "Words SGJP does not know": "| word | meaning | app part of speech |\n|---|---|---|",
    "Verbs SGJP does not know": "| verb | meaning |\n|---|---|",
}


def write(report: Report, path: Path) -> None:
    lines = ["# App grammar vs SGJP", "", "| measure | count |", "|---|---|"]
    lines += [f"| {key} | {value} |" for key, value in sorted(report.counts.items())]
    for section, rows in report.sections.items():
        lines += ["", f"## {section} ({len(rows)})", "", HEADERS[section], *rows]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else TOOLS / ".cache" / "sgjp-report.md"
    words = corpus_entries()
    grammar = shipped_grammar()
    conjugations = json.loads(CONJUGATIONS.read_text(encoding="utf-8"))

    lemmas = {sgjp.verb_lemma(e["word"])[0] for e in words} | {sgjp.verb_lemma(v.get("bezokolicznik", ""))[0] for v in conjugations}
    try:
        lexicon = sgjp.load(lemmas)
    except FileNotFoundError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    report = Report()
    compare_coverage(words, lexicon, report)
    compare_nouns([e for e in words if e["pos"] == "n"], grammar, lexicon, report)
    compare_adjectives([e for e in words if e["pos"] == "adj"], grammar, lexicon, report)
    compare_verbs(conjugations, lexicon, report)
    write(report, target)
    for key, value in sorted(report.counts.items()):
        print(f"{key}: {value}")
    print(f"report: {target}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
