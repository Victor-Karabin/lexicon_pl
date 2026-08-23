#!/usr/bin/env python3
"""Rewrites every shipped English gloss with Google Cloud Translation.

Run from anywhere:  python3 tools/translate/retranslate.py

Reads
    local.properties                        google.translateApiKey, or google.ttsApiKey
    tools/vocabulary/corpus/**/*.tsv        the authored vocabulary, column 2 is the gloss
    data/src/androidMain/assets/conjugations.json

Writes the same corpus files and conjugations.json back, then rebuilds
vocabulary_pl.json through tools/vocabulary/build_assets.py.

Translations are cached in tools/translate/.cache.json, so a second run costs
nothing and an interrupted run resumes. Pass --refresh to ignore the cache.

Bare words translate poorly, so verbs go through the frame "chcę <verb>" and come
back as "to <verb>". A reply that is empty or that echoes the Polish means the API
had nothing to offer, and the existing gloss is kept.

A Polish word carrying two senses in the corpus keeps both authored glosses. One
request cannot tell the senses apart, and collapsing them onto one gloss both loses
a word and breaks the uniqueness the asset build enforces.

A gloss another word already answers to is refused for the same reason: the English
side of the corpus has to stay distinguishable, or word match and true-or-false end
up showing the same answer twice.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CORPUS = ROOT / "tools" / "vocabulary" / "corpus"
CONJUGATIONS = ROOT / "data" / "src" / "androidMain" / "assets" / "conjugations.json"
CACHE = Path(__file__).parent / ".cache.json"
BUILD_ASSETS = ROOT / "tools" / "vocabulary" / "build_assets.py"

ENDPOINT = "https://translation.googleapis.com/language/translate/v2"
VERB_FRAME = "chcę "
VERB_REPLY = "I want "
BATCH = 100
RETRIES = 4


class TranslateError(RuntimeError):
    pass


def api_key() -> str:
    properties = ROOT / "local.properties"
    if not properties.exists():
        raise TranslateError("local.properties is missing; it holds the Cloud Translation key")

    keys = {}
    for line in properties.read_text().splitlines():
        name, sep, value = line.partition("=")
        if sep:
            keys[name.strip()] = value.strip()

    key = keys.get("google.translateApiKey") or keys.get("google.ttsApiKey")
    if not key:
        raise TranslateError("no google.translateApiKey or google.ttsApiKey in local.properties")
    return key


def translate(key: str, texts: list[str]) -> list[str]:
    query = urllib.parse.urlencode(
        [("key", key), ("source", "pl"), ("target", "en"), ("format", "text")]
        + [("q", text) for text in texts]
    )
    for attempt in range(RETRIES):
        try:
            with urllib.request.urlopen(f"{ENDPOINT}?{query}") as reply:
                body = json.load(reply)
            return [item["translatedText"] for item in body["data"]["translations"]]
        except urllib.error.HTTPError as error:
            if error.code not in (429, 500, 503) or attempt == RETRIES - 1:
                raise TranslateError(f"{error.code}: {error.read().decode()[:400]}") from error
            time.sleep(2**attempt)
    raise TranslateError("out of retries")


class Translator:
    def __init__(self, key: str, refresh: bool, dry_run: bool):
        self.key = key
        self.dry_run = dry_run
        self.cache: dict[str, str] = {}
        if CACHE.exists() and not refresh:
            self.cache = json.loads(CACHE.read_text())
        self.calls = 0

    def warm(self, phrases: list[str]) -> None:
        pending = sorted({phrase for phrase in phrases if phrase not in self.cache})
        for start in range(0, len(pending), BATCH):
            batch = pending[start : start + BATCH]
            if self.dry_run:
                print(f"  would translate {len(batch)} phrases")
                continue
            for phrase, result in zip(batch, translate(self.key, batch)):
                self.cache[phrase] = result
            self.calls += len(batch)
            print(f"  {min(start + BATCH, len(pending))}/{len(pending)}", end="\r", flush=True)
        if pending and not self.dry_run:
            self.save()
            print()

    def save(self) -> None:
        CACHE.write_text(json.dumps(self.cache, ensure_ascii=False, indent=0, sort_keys=True))

    def gloss(self, polish: str, existing: str, is_verb: bool) -> str:
        phrase = VERB_FRAME + polish if is_verb else polish
        raw = self.cache.get(phrase)
        if raw is None:
            return existing

        text = raw.strip()
        if is_verb and text.startswith(VERB_REPLY):
            text = text[len(VERB_REPLY) :].strip()
        if not text or text.casefold() == polish.casefold():
            return existing
        if not polish[:1].isupper() and text[:1].isupper() and not text[1:].isupper():
            text = text[:1].lower() + text[1:]
        if text.isupper() and not polish.isupper():
            text = text.lower()
        return text


def corpus_files() -> list[Path]:
    return sorted(CORPUS.rglob("*.tsv"))


def ambiguous_words(files: list[Path]) -> set[str]:
    seen: dict[str, int] = {}
    for path in files:
        for _, columns in corpus_rows(path):
            word = columns[0].strip().casefold()
            seen[word] = seen.get(word, 0) + 1
    return {word for word, count in seen.items() if count > 1}


def corpus_rows(path: Path) -> list[tuple[int, list[str]]]:
    rows = []
    for number, line in enumerate(path.read_text().splitlines()):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        columns = line.split("\t")
        if len(columns) >= 3:
            rows.append((number, columns))
    return rows


def phrases_for(path: Path) -> list[str]:
    return [
        (VERB_FRAME + columns[0].strip() if columns[2].strip() == "v" else columns[0].strip())
        for _, columns in corpus_rows(path)
    ]


def rewrite_corpus(
    path: Path,
    translator: Translator,
    ambiguous: set[str],
    taken: set[str],
) -> int:
    lines = path.read_text().splitlines()
    changed = 0
    for number, columns in corpus_rows(path):
        polish = columns[0].strip()
        authored = columns[1].strip()
        if polish.casefold() in ambiguous:
            continue

        gloss = translator.gloss(polish, authored, columns[2].strip() == "v")
        if gloss.casefold() in taken:
            gloss = authored
        taken.add(gloss.casefold())
        if gloss == authored:
            continue

        columns[1] = gloss
        lines[number] = "\t".join(column.rstrip("\n") for column in columns)
        changed += 1
    if changed:
        path.write_text("\n".join(lines) + "\n")
    return changed


def rewrite_conjugations(translator: Translator) -> int:
    verbs = json.loads(CONJUGATIONS.read_text())
    changed = 0
    for verb in verbs:
        gloss = translator.gloss(verb["bezokolicznik"], verb.get("translation", ""), is_verb=True)
        if gloss != verb.get("translation"):
            verb["translation"] = gloss
            changed += 1
    if changed:
        CONJUGATIONS.write_text(json.dumps(verbs, ensure_ascii=False, indent=2) + "\n")
    return changed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="report what would be sent, translate nothing")
    parser.add_argument("--refresh", action="store_true", help="ignore the cache and re-ask for everything")
    parser.add_argument("--skip-build", action="store_true", help="leave vocabulary_pl.json to a later build")
    arguments = parser.parse_args()

    try:
        translator = Translator(api_key(), refresh=arguments.refresh, dry_run=arguments.dry_run)
    except TranslateError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    files = corpus_files()
    verbs = json.loads(CONJUGATIONS.read_text())

    wanted = [phrase for path in files for phrase in phrases_for(path)]
    wanted += [VERB_FRAME + verb["bezokolicznik"] for verb in verbs]
    print(f"{len(set(wanted))} distinct phrases across {len(files)} corpus files and {len(verbs)} verbs")

    try:
        translator.warm(wanted)
    except TranslateError as error:
        translator.save()
        print(f"error: {error}", file=sys.stderr)
        return 1

    if arguments.dry_run:
        return 0

    total = 0
    ambiguous = ambiguous_words(files)
    if ambiguous:
        print(f"keeping the authored glosses for {len(ambiguous)} words carrying two senses")

    taken = {
        columns[1].strip().casefold()
        for path in files
        for _, columns in corpus_rows(path)
    }
    for path in files:
        changed = rewrite_corpus(path, translator, ambiguous, taken)
        total += changed
        if changed:
            print(f"  {path.relative_to(ROOT)}: {changed} glosses")

    changed = rewrite_conjugations(translator)
    total += changed
    print(f"  conjugations.json: {changed} glosses")
    print(f"{total} glosses rewritten")

    if not arguments.skip_build:
        print("Rebuilding vocabulary_pl.json…")
        return subprocess.run([sys.executable, str(BUILD_ASSETS)], check=False).returncode
    return 0


if __name__ == "__main__":
    sys.exit(main())
