#!/usr/bin/env python3
"""Writes an example sentence for every shipped word and verb, using OpenAI.

Run from anywhere:  python3 tools/vocabulary/build_examples.py

Reads
    local.properties                        openai.apiKey
    tools/vocabulary/corpus/**/*.tsv        the words that need a sentence
    data/src/androidMain/assets/conjugations.json

Writes
    tools/vocabulary/examples.tsv           word, gloss, sentence
    data/src/androidMain/assets/conjugations.json

Then run build_assets.py to fold examples.tsv into vocabulary_pl.json.

The target word is wrapped in double asterisks in whatever form the sentence needs,
so the app can bold it without having to guess at Polish inflection. A reply that
loses the markers, or marks more than one span, is dropped rather than shipped.
Checking that the marked span belongs to the word asked about is not possible from
the spelling: Polish alternates stems, so brać becomes biorę and iść becomes idę.

Sentences are cached in tools/vocabulary/.examples-cache.json, so a second run costs
nothing and an interrupted run resumes. Pass --refresh to ask again for everything.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = Path(__file__).parent
CORPUS = TOOLS / "corpus"
EXAMPLES = TOOLS / "examples.tsv"
CONJUGATIONS = ROOT / "data" / "src" / "androidMain" / "assets" / "conjugations.json"
CACHE = TOOLS / ".examples-cache.json"

ENDPOINT = "https://api.openai.com/v1/responses"
MODEL = "gpt-5.4-mini"
BATCH = 25
RETRIES = 4

OTHER_HEADWORDS: set[str] = set()

PROMPT = """For each Polish entry below write ONE short, natural Polish example sentence.

Rules:
* Eight words or fewer wherever the sentence still sounds natural.
* Keep the vocabulary and grammar at or below the CEFR level given for the entry.
* Inflect the target word as the grammar requires and wrap that form in double asterisks.
* Never replace the target word with a synonym, and never mark anything else.
* Make the meaning of the target word clear from the sentence itself.
* Prefer everyday situations over literary, archaic or regional language.

Return ONLY a JSON object mapping each target word, exactly as given, to its sentence.

Entries, one per line, as "word | meaning | level":

{{entries}}
"""


class BuildError(RuntimeError):
    pass


def api_key() -> str:
    properties = ROOT / "local.properties"
    if not properties.exists():
        raise BuildError("local.properties is missing; it holds the OpenAI key")

    for line in properties.read_text().splitlines():
        name, sep, value = line.partition("=")
        if sep and name.strip() == "openai.apiKey" and value.strip():
            return value.strip()
    raise BuildError("no openai.apiKey in local.properties")


def ask(key: str, entries: list[tuple[str, str, str]]) -> dict[str, str]:
    prompt = PROMPT.replace(
        "{{entries}}", "\n".join(f"{word} | {gloss} | {level}" for word, gloss, level in entries)
    )
    body = json.dumps(
        {
            "model": MODEL,
            "input": [{"role": "developer", "content": [{"type": "input_text", "text": prompt}]}],
        }
    ).encode()

    for attempt in range(RETRIES):
        request = urllib.request.Request(
            ENDPOINT,
            data=body,
            headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(request) as reply:
                answer = json.load(reply)
            return parse(answer)
        except urllib.error.HTTPError as error:
            if error.code not in (429, 500, 502, 503) or attempt == RETRIES - 1:
                raise BuildError(f"{error.code}: {error.read().decode()[:300]}") from error
            time.sleep(2**attempt)
        except (urllib.error.URLError, TimeoutError) as error:
            if attempt == RETRIES - 1:
                raise BuildError(str(error)) from error
            time.sleep(2**attempt)
    raise BuildError("out of retries")


def parse(answer: dict) -> dict[str, str]:
    text = answer.get("output_text") or next(
        (
            content["text"]
            for output in answer.get("output", [])
            for content in output.get("content", [])
            if content.get("text")
        ),
        "",
    )
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end < start:
        return {}
    try:
        parsed = json.loads(text[start : end + 1])
    except json.JSONDecodeError:
        return {}
    return {k: v for k, v in parsed.items() if isinstance(v, str)}


def usable(word: str, sentence: str) -> bool:
    """One marked span, and it is not some other entry's word verbatim.

    Nothing stronger is available: an inflected Polish form need not share a prefix
    with its infinitive, so brać marked as biorę is correct and unrecognisable.
    """
    marked = re.findall(r"\*\*(.+?)\*\*", sentence)
    if len(marked) != 1 or not marked[0].strip():
        return False

    return marked[0].strip().casefold() not in OTHER_HEADWORDS - {word.casefold()}


def read_tsv(path: Path) -> list[list[str]]:
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            rows.append(line.split("\t"))
    return rows


def corpus_entries() -> list[tuple[str, str, str]]:
    entries = []
    for path in [CORPUS / "core.tsv", *sorted((CORPUS / "topics").glob("*.tsv"))]:
        for cols in read_tsv(path):
            if len(cols) >= 4:
                entries.append((cols[0].strip(), cols[1].strip(), cols[3].strip()))
    return entries


def verb_entries(verbs: list[dict]) -> list[tuple[str, str, str]]:
    return [(v["bezokolicznik"], v.get("translation", ""), "B1") for v in verbs if v.get("bezokolicznik")]


class Writer:
    def __init__(self, refresh: bool):
        self.cache: dict[str, str] = {}
        if CACHE.exists() and not refresh:
            self.cache = json.loads(CACHE.read_text(encoding="utf-8"))
        self.rejected = 0

    def save(self) -> None:
        CACHE.write_text(json.dumps(self.cache, ensure_ascii=False, indent=0, sort_keys=True), encoding="utf-8")

    def warm(self, key: str, entries: list[tuple[str, str, str]], label: str) -> None:
        pending = [e for e in entries if e[0] not in self.cache]
        if not pending:
            print(f"  {label}: already written")
            return

        for start in range(0, len(pending), BATCH):
            batch = pending[start : start + BATCH]
            written = ask(key, batch)
            for word, _, _ in batch:
                sentence = written.get(word, "").strip()
                self.cache[word] = sentence if usable(word, sentence) else ""
                if not self.cache[word]:
                    self.rejected += 1
            print(f"  {label}: {min(start + BATCH, len(pending))}/{len(pending)}", end="\r", flush=True)
            if start % (BATCH * 20) == 0:
                self.save()
        self.save()
        print()

    def sentence(self, word: str) -> str:
        return self.cache.get(word, "")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh", action="store_true", help="ignore the cache and ask again for everything")
    parser.add_argument("--words-only", action="store_true", help="skip the verb list")
    parser.add_argument("--verbs-only", action="store_true", help="skip the corpus")
    arguments = parser.parse_args()

    try:
        key = api_key()
    except BuildError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    writer = Writer(refresh=arguments.refresh)
    words = corpus_entries()
    verbs = json.loads(CONJUGATIONS.read_text(encoding="utf-8"))

    OTHER_HEADWORDS.update(word.casefold() for word, _, _ in words)
    OTHER_HEADWORDS.update(v["bezokolicznik"].casefold() for v in verbs if v.get("bezokolicznik"))

    print(f"{len(words)} corpus words, {len(verbs)} verbs")
    try:
        if not arguments.verbs_only:
            writer.warm(key, words, "words")
        if not arguments.words_only:
            writer.warm(key, verb_entries(verbs), "verbs")
    except BuildError as error:
        writer.save()
        print(f"error: {error}", file=sys.stderr)
        return 1

    if not arguments.verbs_only:
        lines = ["# Generated by build_examples.py. Columns: polish <TAB> english <TAB> sentence."]
        written = 0
        for word, gloss, _ in words:
            sentence = writer.sentence(word)
            if sentence:
                lines.append(f"{word}\t{gloss}\t{sentence}")
                written += 1
        EXAMPLES.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"examples.tsv: {written} of {len(words)} words")

    if not arguments.words_only:
        written = 0
        for verb in verbs:
            sentence = writer.sentence(verb.get("bezokolicznik", ""))
            if sentence:
                verb["example"] = sentence
                written += 1
        CONJUGATIONS.write_text(json.dumps(verbs, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"conjugations.json: {written} of {len(verbs)} verbs")

    if writer.rejected:
        print(f"{writer.rejected} replies dropped for not marking the word they were asked about")
    return 0


if __name__ == "__main__":
    sys.exit(main())
