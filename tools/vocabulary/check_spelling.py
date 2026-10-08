#!/usr/bin/env python3
"""Lists every word in the shipped Polish text that no dictionary knows.

Run from anywhere:  python3 tools/vocabulary/check_spelling.py

Reads
    tools/vocabulary/.cache/sgjp-*.tab.gz and polimorf-*.tab.gz   see sgjp.py
    tools/vocabulary/corpus/**/*.tsv        headwords and phrases
    tools/vocabulary/examples.tsv           example sentences
    data/src/androidMain/assets/conjugations.json

Prints each unknown word with the entries it appears in, most frequent first. With
--drop, also removes every example sentence holding an unknown word from
examples.tsv and .examples-cache.json, so the next build_examples.py run writes it again. The
example sentences were written by OpenAI and the corpus partly by hand; SGJP and PoliMorf
together list every inflected form of about 400,000 Polish words, so a form neither knows
is nearly always a typo or an invented word. Capitalised words inside a sentence are names
and are not checked.
"""

from __future__ import annotations

import gzip
import json
import re
import sys
from collections import defaultdict

import sgjp
import tatoeba
from build_examples import CONJUGATIONS, TOOLS, corpus_entries, read_tsv

EXAMPLES_CACHE = TOOLS / ".examples-cache.json"

ACCEPTED = {"się", "siebie"}

TOKEN = re.compile(r"[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż]+(?:-[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż]+)*")


def texts() -> list[tuple[str, str, str | None]]:
    found = [(entry["word"], f"word {entry['word']}", None) for entry in corpus_entries()]
    found += [(cols[2], f"example of {cols[0]}", f"{cols[0]}\t{cols[1]}") for cols in read_tsv(TOOLS / "examples.tsv") if len(cols) >= 3]
    for verb in json.loads(CONJUGATIONS.read_text(encoding="utf-8")):
        if verb.get("example"):
            infinitive = verb.get("bezokolicznik", "")
            found.append((verb["example"], f"example of {infinitive}", f"{infinitive}\t{verb.get('translation', '')}"))
    return found


def accepted() -> set[str]:
    deliberate = set(ACCEPTED)
    for entry in corpus_entries():
        deliberate.update(word.lower() for word in TOKEN.findall(entry["word"]))
    for cols in read_tsv(TOOLS / "unlisted_verbs.tsv"):
        for cell in [cols[0], *cols[2:]]:
            deliberate.update(word.lower() for word in TOKEN.findall(cell))
    return deliberate


def words(text: str) -> list[str]:
    plain = text.replace("**", "")
    return [
        token
        for index, token in enumerate(TOKEN.findall(plain))
        if (index == 0 or not token[0].isupper()) and not tatoeba.CONDITIONAL.match(token.lower())
    ]


def known(forms: set[str]) -> set[str]:
    found = set()
    for name in ("sgjp", "polimorf"):
        with gzip.open(sgjp.dictionary_file(name), "rt", encoding="utf-8") as lines:
            for line in lines:
                form = line.split("\t", 1)[0]
                if form in forms or form.lower() in forms:
                    found.add(form.lower())
    return found


def main() -> int:
    drop = "--drop" in sys.argv[1:]
    where: dict[str, list[str]] = defaultdict(list)
    cache_keys: dict[str, set[str]] = defaultdict(set)
    for text, label, key in texts():
        for word in words(text):
            where[word.lower()].append(label)
            if key:
                cache_keys[word.lower()].add(key)
    try:
        dictionary = known(set(where))
    except FileNotFoundError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    dictionary |= accepted()
    unknown = sorted(((word, labels) for word, labels in where.items() if word not in dictionary), key=lambda it: -len(it[1]))
    for word, labels in unknown:
        print(f"{word}\t{len(labels)}\t{'; '.join(dict.fromkeys(labels[:5]))}")
    print(f"{len(unknown)} unknown forms in {len(where)} checked", file=sys.stderr)
    if drop and unknown and EXAMPLES_CACHE.exists():
        cache = json.loads(EXAMPLES_CACHE.read_text(encoding="utf-8"))
        keys = {key for word, _ in unknown for key in cache_keys[word]}
        stale = (keys | {key.split("\t")[0] for key in keys}) & set(cache)
        for key in stale:
            del cache[key]
        EXAMPLES_CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=0, sort_keys=True), encoding="utf-8")
        examples = TOOLS / "examples.tsv"
        kept = [line for line in examples.read_text(encoding="utf-8").splitlines() if "\t".join(line.split("\t")[:2]) not in keys]
        examples.write_text("\n".join(kept) + "\n", encoding="utf-8")
        print(f"dropped {len(stale)} sentences from {EXAMPLES_CACHE.name}; run build_examples.py to write them again", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
