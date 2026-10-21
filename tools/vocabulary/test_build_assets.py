#!/usr/bin/env python3
"""Tests for the vocabulary build's validator:  python3 tools/vocabulary/test_build_assets.py

The validator runs before anything is written, so these assert what it reports on small
synthetic corpora, plus one pass over the shipped asset to hold it to the same rule.
"""

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from build_assets import ASSETS, CEFR_LEVELS, validate  # noqa: E402

CYRILLIC = re.compile("[\u0400-\u04FF]")

FAILURES = []


def check(name, condition):
    if not condition:
        FAILURES.append(name)


def word(number, text="kot", translation="cat", cefr="A1"):
    return {"id": number, "text": text, "translation": translation, "cefr": cefr}


def corpus(*extra):
    return [word(index, text=f"slowo{index}", cefr=level) for index, level in enumerate(CEFR_LEVELS, start=1)] + list(extra)


def cyrillic_problems(words):
    return [problem for problem in validate(words, [], []) if "cyrillic" in problem.lower()]


def test_clean_corpus_reports_no_cyrillic():
    check("no problem on a clean corpus", cyrillic_problems(corpus(word(100))) == [])


def test_cyrillic_in_a_translation_is_reported():
    problems = cyrillic_problems(corpus(word(100, text="w oczekiwaniu na", translation="in \u043e of")))
    check("one problem", len(problems) == 1)
    check("names the word", problems and "w oczekiwaniu na" in problems[0])


def test_cyrillic_in_a_polish_text_is_reported():
    check("reported", len(cyrillic_problems(corpus(word(100, text="k\u043et")))) == 1)


def test_cyrillic_in_an_example_is_reported():
    broken = word(100)
    broken["example"] = "Ten kot jest \u043e duży."
    check("reported", len(cyrillic_problems(corpus(broken))) == 1)


def test_polish_diacritics_are_not_cyrillic():
    check("no problem", cyrillic_problems(corpus(word(100, text="zażółć gęślą jaźń", translation="pangram"))) == [])


def test_shipped_vocabulary_has_no_cyrillic():
    shipped = json.loads((ASSETS / "vocabulary_pl.json").read_text(encoding="utf-8"))
    offenders = [w["text"] for w in shipped if CYRILLIC.search(json.dumps(w, ensure_ascii=False))]
    check(f"Cyrillic in the shipped asset: {offenders}", offenders == [])
    check("the validator accepts the shipped asset", cyrillic_problems(shipped) == [])


def test_no_source_tsv_has_cyrillic():
    sources = sorted(Path(__file__).parent.glob("*.tsv")) + sorted(Path(__file__).parent.glob("corpus/**/*.tsv"))
    offenders = [
        f"{path.name}:{number}"
        for path in sources
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1)
        if CYRILLIC.search(line)
    ]
    check(f"Cyrillic in source files: {offenders}", offenders == [])


def test_awaiting_is_glossed_in_english():
    shipped = json.loads((ASSETS / "vocabulary_pl.json").read_text(encoding="utf-8"))
    gloss = next(w["translation"] for w in shipped if w["text"] == "w oczekiwaniu na")
    check(f"gloss is {gloss!r}", gloss == "awaiting / in anticipation of")


def main():
    for name, test in sorted(globals().items()):
        if name.startswith("test_") and callable(test):
            try:
                test()
            except Exception as error:  # noqa: BLE001 - a raising test is a failing test
                FAILURES.append(f"{name}: raised {error!r}")

    if FAILURES:
        print(f"{len(FAILURES)} failed:")
        for failure in FAILURES:
            print(f"  {failure}")
        return 1
    print("all vocabulary build tests passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
