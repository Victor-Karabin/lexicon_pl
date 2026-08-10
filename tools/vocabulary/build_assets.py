#!/usr/bin/env python3
"""Builds the shipped vocabulary and preset assets from the authored corpus.

Run from anywhere:  python3 tools/vocabulary/build_assets.py

Reads
    tools/vocabulary/corpus/core.tsv        frequency-ordered core list (line order = rank)
    tools/vocabulary/corpus/topics/*.tsv    topical vocabulary beyond the core list
    tools/vocabulary/categories.tsv         preset categories
    tools/vocabulary/presets.tsv            preset metadata and selection rules

Writes
    data/src/main/assets/vocabulary_pl.json
    data/src/main/assets/vocabulary_presets.json

Selection rules resolve to explicit id lists at build time, so the app only ever reads
a list of ids and never re-implements "what counts as A2" or "what counts as top 500".
Validation runs before anything is written: a corpus that would ship a broken preset
fails the build here rather than at runtime on a user's phone.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from g2p import transcribe  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools" / "vocabulary"
ASSETS = ROOT / "data" / "src" / "main" / "assets"

CEFR_LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]
POS_TAGS = {"n", "v", "adj", "adv", "prn", "num", "prep", "conj", "part", "interj", "expr"}

# Below this a word carries no spelling to practise; see the check in load_words.
MIN_WORD_LENGTH = 3

# A minute per word is the usual rule of thumb for first exposure plus a review pass.
SECONDS_PER_WORD = 60


class BuildError(Exception):
    """A problem the corpus author has to fix; never raised at runtime in the app."""


def read_tsv(path: Path) -> list[list[str]]:
    rows = []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip() or line.startswith("#"):
            continue
        rows.append((number, line.split("\t")))
    return rows


def load_words() -> list[dict]:
    """Core first so ids follow frequency, then topical files in a stable order."""
    words: list[dict] = []
    seen: dict[tuple[str, str], int] = {}

    sources = [(TOOLS / "corpus" / "core.tsv", True)]
    sources += [(p, False) for p in sorted((TOOLS / "corpus" / "topics").glob("*.tsv"))]

    for path, is_core in sources:
        for number, cols in read_tsv(path):
            if len(cols) < 4:
                raise BuildError(f"{path.name}:{number}: expected at least 4 columns, got {len(cols)}")
            text, translation, pos, cefr = (c.strip() for c in cols[:4])
            topics = [t.strip() for t in cols[4].split(",")] if len(cols) > 4 and cols[4].strip() else []

            if not text or not translation:
                raise BuildError(f"{path.name}:{number}: empty word or translation")
            if pos not in POS_TAGS:
                raise BuildError(f"{path.name}:{number}: unknown part of speech '{pos}'")
            if cefr not in CEFR_LEVELS:
                raise BuildError(f"{path.name}:{number}: unknown CEFR level '{cefr}'")
            # One- and two-letter entries are function words, and they break the trainings
            # built on spelling: a two-letter crossword answer or letter puzzle is not an
            # exercise. Rejected here so they cannot drift back in.
            if len(text) <= MIN_WORD_LENGTH - 1:
                raise BuildError(f"{path.name}:{number}: '{text}' is shorter than {MIN_WORD_LENGTH} letters")

            # Homonyms are legitimate (bez = without / lilac), so identity is the pair.
            key = (text.lower(), translation.lower())
            if key in seen:
                raise BuildError(f"{path.name}:{number}: '{text} = {translation}' already defined")
            seen[key] = number

            words.append(
                {
                    "id": len(words) + 1,
                    "text": text,
                    "translation": translation,
                    "transcription": transcribe(text),
                    "partOfSpeech": pos,
                    "cefr": cefr,
                    "topics": topics,
                    # Rank exists only for the core list; topical extras are not ranked.
                    "frequencyRank": len(words) + 1 if is_core else None,
                }
            )
    return words


def load_categories() -> list[dict]:
    categories = []
    for number, cols in read_tsv(TOOLS / "categories.tsv"):
        if len(cols) != 4:
            raise BuildError(f"categories.tsv:{number}: expected 4 columns, got {len(cols)}")
        cid, order, title_en, title_pl = (c.strip() for c in cols)
        categories.append({"id": cid, "order": int(order), "title": {"en": title_en, "pl": title_pl}})
    return categories


def select_ids(rule: str, arg: str, words: list[dict]) -> list[int]:
    if rule == "frequency":
        ranked = [w for w in words if w["frequencyRank"] is not None]
        return [w["id"] for w in sorted(ranked, key=lambda w: w["frequencyRank"])[: int(arg)]]
    if rule == "topic":
        return [w["id"] for w in words if arg in w["topics"]]
    if rule == "pos":
        return [w["id"] for w in words if w["partOfSpeech"] == arg]
    raise BuildError(f"unknown selection rule '{rule}'")


def load_presets(words: list[dict], categories: list[dict]) -> list[dict]:
    known_categories = {c["id"] for c in categories}
    presets = []
    for number, cols in read_tsv(TOOLS / "presets.tsv"):
        if len(cols) != 11:
            raise BuildError(f"presets.tsv:{number}: expected 11 columns, got {len(cols)}")
        pid, category, rule, arg, popularity, icon, color, t_en, t_pl, d_en, d_pl = (c.strip() for c in cols)

        if category not in known_categories:
            raise BuildError(f"presets.tsv:{number}: unknown category '{category}'")

        ids = select_ids(rule, arg, words)
        presets.append(
            {
                "id": pid,
                "category": category,
                "title": {"en": t_en, "pl": t_pl},
                "description": {"en": d_en, "pl": d_pl},
                "icon": icon,
                "color": color,
                "popularity": int(popularity),
                "estimatedSeconds": len(ids) * SECONDS_PER_WORD,
                "wordCount": len(ids),
                "vocabularyIds": ids,
            }
        )
    return presets


def validate(words: list[dict], presets: list[dict], categories: list[dict]) -> list[str]:
    """Every rule the specification asks for, checked against the built data."""
    problems: list[str] = []

    ids = [w["id"] for w in words]
    if len(set(ids)) != len(ids):
        problems.append("vocabulary contains duplicate ids")

    preset_ids = [p["id"] for p in presets]
    for duplicate in {p for p in preset_ids if preset_ids.count(p) > 1}:
        problems.append(f"duplicate preset id '{duplicate}'")

    known_words = set(ids)
    known_categories = {c["id"] for c in categories}
    for preset in presets:
        missing = [i for i in preset["vocabularyIds"] if i not in known_words]
        if missing:
            problems.append(f"preset '{preset['id']}' references unknown words {missing[:5]}")
        if not preset["vocabularyIds"]:
            problems.append(f"preset '{preset['id']}' is empty")
        if len(set(preset["vocabularyIds"])) != len(preset["vocabularyIds"]):
            problems.append(f"preset '{preset['id']}' lists the same word twice")
        if preset["category"] not in known_categories:
            problems.append(f"preset '{preset['id']}' has invalid category '{preset['category']}'")

        # A preset the user cannot practise is worse than no preset: every training needs
        # enough material to build a step, and Image Test needs six distinct options.
        if 0 < preset["wordCount"] < 6:
            problems.append(f"preset '{preset['id']}' has only {preset['wordCount']} words, too few to train")

    # Every level is offered as a filter chip, so a level with no words is a control that
    # silently does nothing — which is how C2 shipped empty.
    by_level = {level: sum(1 for w in words if w["cefr"] == level) for level in CEFR_LEVELS}
    for level, count in by_level.items():
        if count == 0:
            problems.append(f"CEFR level {level} has no words, so its filter would return nothing")

    return problems


def main() -> int:
    try:
        words = load_words()
        categories = load_categories()
        presets = load_presets(words, categories)
    except BuildError as error:
        print(f"corpus error: {error}", file=sys.stderr)
        return 1

    problems = validate(words, presets, categories)
    if problems:
        print("validation failed:", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1

    ASSETS.mkdir(parents=True, exist_ok=True)
    vocabulary_asset = [
        {k: w[k] for k in ("id", "text", "translation", "transcription", "partOfSpeech", "cefr", "topics")}
        for w in words
    ]
    (ASSETS / "vocabulary_pl.json").write_text(
        json.dumps(vocabulary_asset, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )
    (ASSETS / "vocabulary_presets.json").write_text(
        json.dumps({"categories": categories, "presets": presets}, ensure_ascii=False, indent=1) + "\n",
        encoding="utf-8",
    )

    ranked = sum(1 for w in words if w["frequencyRank"] is not None)
    print(f"{len(words)} words ({ranked} ranked), {len(presets)} presets in {len(categories)} categories")
    levels = {level: sum(1 for w in words if w["cefr"] == level) for level in CEFR_LEVELS}
    print("  by level: " + ", ".join(f"{lvl} {n}" for lvl, n in levels.items()))
    smallest = min(presets, key=lambda p: p["wordCount"])
    print(f"smallest preset: {smallest['id']} ({smallest['wordCount']} words)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
