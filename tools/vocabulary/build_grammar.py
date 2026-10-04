#!/usr/bin/env python3
"""Writes the grammar every shipped word needs, using OpenAI.

Run from anywhere:  python3 tools/vocabulary/build_grammar.py

Reads
    local.properties                        openai.apiKey
    tools/vocabulary/corpus/**/*.tsv        the words and their part of speech
    tools/vocabulary/examples.tsv           each word's example sentence, as context
    data/src/androidMain/assets/conjugations.json

Writes
    tools/vocabulary/grammar.tsv            word, gloss, gender, plural, adjective forms
    data/src/androidMain/assets/conjugations.json   conjugations for verbs that had none

Then run build_assets.py to fold grammar.tsv into vocabulary_pl.json.

A learner meeting kot needs to know it is a masculine noun that goes kot, kota, kotu, and
meeting dobry needs dobra, dobre and dobrzy. The part of speech already ships; the
forms did not exist anywhere. Verbs are the exception: conjugations.json already holds
the present tense for most of them, and the verb trainings read it, so the verbs
missing from it are written back into that same file rather than into a second place.

Answers are cached in tools/vocabulary/.grammar-cache.json. Pass --refresh to ask again.
"""

from __future__ import annotations

import argparse
import json
import sys

from build_examples import BATCH, CONJUGATIONS, TOOLS, BuildError, api_key, corpus_entries, entry_key, entry_lines, read_tsv, request

GRAMMAR = TOOLS / "grammar.tsv"
CACHE = TOOLS / ".grammar-cache.json"

GENDERS = {"masculine personal", "masculine animate", "masculine inanimate", "feminine", "neuter", "plural only"}

CASES = ["nominative", "genitive", "dative", "accusative", "instrumental", "locative", "vocative"]

PERSONS = ["ja", "ty", "on/ona/ono", "my", "wy", "oni/one"]

NOUN_PROMPT = """For each Polish noun below, give its gender and its declension.

Rules:
* gender is exactly one of: masculine personal, masculine animate, masculine inanimate,
  feminine, neuter, plural only.
  nauczyciel is masculine personal, pies is masculine animate, stół is masculine inanimate,
  drzwi is plural only.
* Decline it through all seven cases, singular then plural, as a Pole would write it:
  kot -> kot/koty, kota/kotów, kotu/kotom, kota/koty, kotem/kotami, kocie/kotach, kocie/koty.
  Mind the irregulars and the stem changes: człowiek -> ludzie, ręka -> ręce, dziecko -> dzieci,
  stół -> stole, pies -> psa.
* Leave the plural of each case empty when the noun has no plural in normal use, such as
  mass nouns like mleko or abstract nouns like wolność. For a plural-only noun such as
  drzwi, leave every singular empty instead.
* Give the forms for the meaning stated, not another sense of the same spelling.

Return ONLY a JSON object mapping each entry number, as a string, to an object
{"gender": "...", "nominative": "singular/plural", "genitive": "...", "dative": "...",
"accusative": "...", "instrumental": "...", "locative": "...", "vocative": "..."},
where each case is the singular and the plural separated by a slash.

Entries, one per line, as "number | word | meaning | topics | example":

{{entries}}
"""

ADJECTIVE_PROMPT = """For each Polish adjective below, give its nominative forms.

Rules:
* masculine, feminine and neuter singular, then the two plurals Polish distinguishes:
  the masculine personal plural (męskoosobowy, used of groups including men) and the
  plural for everything else (niemęskoosobowy).
  dobry -> dobry, dobra, dobre, dobrzy, dobre. duży -> duży, duża, duże, duzi, duże.
  Mind the consonant changes: wysoki -> wysocy, młody -> młodzi, drogi -> drodzy.
* Entries given in a non-masculine form belong to the same paradigm: answer with the
  whole set, starting from the masculine.
* An adjective that does not inflect, and adverbs filed here by mistake, get five empty
  strings.

Return ONLY a JSON object mapping each entry number, as a string, to an object
{"masculine": "...", "feminine": "...", "neuter": "...", "pluralPersonal": "...", "pluralOther": "..."}.

Entries, one per line, as "number | word | meaning | topics | example":

{{entries}}
"""

VERB_PROMPT = """For each Polish verb below, give the present tense, all six persons.

Rules:
* ja, ty, on/ona/ono, my, wy, oni/one, for the verb exactly as given, keeping się where
  the entry has it: martwić się -> martwię się, martwisz się, ...
* A perfective verb has no present tense: give its simple future instead, which is what
  those forms express (kupić -> kupię, kupisz, kupi, kupimy, kupicie, kupią).
* An impersonal verb such as trzeba or można has one form: put it under on/ona/ono and
  leave the other five empty.

Return ONLY a JSON object mapping each entry number, as a string, to an object with the
keys "ja", "ty", "on/ona/ono", "my", "wy", "oni/one".

Entries, one per line, as "number | word | meaning | topics | example":

{{entries}}
"""


def examples() -> dict[str, str]:
    path = TOOLS / "examples.tsv"
    if not path.exists():
        return {}
    return {f"{cols[0]}\t{cols[1]}": cols[2] for cols in read_tsv(path) if len(cols) >= 3}


def ask(key: str, entries: list[dict], prompt: str) -> dict[str, dict]:
    filled = prompt.replace("{{entries}}", entry_lines(entries, ["word", "gloss", "topics", "example"]))
    answer = request(key, filled)
    return {
        entry_key(entries[int(n) - 1]): reply
        for n, reply in answer.items()
        if isinstance(reply, dict) and n.isdigit() and 0 < int(n) <= len(entries)
    }


def cleaned(reply: dict, fields: list[str]) -> dict[str, str]:
    return {field: str(reply.get(field, "") or "").strip() for field in fields}


def usable_noun(reply: dict[str, str]) -> bool:
    return reply.get("gender", "") in GENDERS


def warm(key: str, pending: list[dict], prompt: str, fields: list[str], cache: dict, label: str) -> None:
    if not pending:
        print(f"  {label}: already written")
        return

    for start in range(0, len(pending), BATCH):
        batch = pending[start : start + BATCH]
        written = ask(key, batch, prompt)
        for entry in batch:
            reply = written.get(entry_key(entry))
            if isinstance(reply, dict):
                cache[entry_key(entry)] = cleaned(reply, fields)
        print(f"  {label}: {min(start + BATCH, len(pending))}/{len(pending)}", end="\r", flush=True)
        save(cache)
    print()


def save(cache: dict) -> None:
    CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=0, sort_keys=True), encoding="utf-8")


def write_grammar(words: list[dict], cache: dict) -> None:
    lines = [
        "# Generated by build_grammar.py. Columns: polish <TAB> english <TAB> gender <TAB> declension "
        "(seven cases, ; between cases, singular|plural) <TAB> masculine <TAB> feminine <TAB> neuter "
        "<TAB> plural personal <TAB> plural other."
    ]
    nouns = adjectives = 0
    for entry in words:
        answer = cache.get(entry_key(entry))
        if not answer:
            continue
        if entry["pos"] == "n" and usable_noun(answer):
            cases = ";".join(answer.get(case, "").replace("/", "|") for case in CASES)
            lines.append(f"{entry['word']}\t{entry['gloss']}\t{answer['gender']}\t{cases}\t\t\t\t\t")
            nouns += 1
        elif entry["pos"] == "adj" and answer.get("masculine"):
            forms = [answer["masculine"], answer["feminine"], answer["neuter"], answer["pluralPersonal"], answer["pluralOther"]]
            lines.append(f"{entry['word']}\t{entry['gloss']}\t\t\t" + "\t".join(forms))
            adjectives += 1
    GRAMMAR.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"grammar.tsv: {nouns} nouns, {adjectives} adjectives")


def write_conjugations(verbs: list[dict], cache: dict, sentences: dict[str, str]) -> None:
    entries = json.loads(CONJUGATIONS.read_text(encoding="utf-8"))
    known = {entry.get("bezokolicznik") for entry in entries}
    added = 0
    for entry in verbs:
        answer = cache.get(entry_key(entry))
        if entry["word"] in known or not answer or not any(answer.get(person) for person in PERSONS):
            continue
        entries.append(
            {
                "bezokolicznik": entry["word"],
                **{person: answer.get(person, "") for person in PERSONS},
                "translation": entry["gloss"],
                "example": sentences.get(entry_key(entry), ""),
            }
        )
        added += 1
    if added:
        entries.sort(key=lambda it: it.get("bezokolicznik", ""))
        CONJUGATIONS.write_text(json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"conjugations.json: {added} verbs added, {len(entries)} in total")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh", action="store_true", help="ignore the cache and ask again for everything")
    arguments = parser.parse_args()

    try:
        key = api_key()
    except BuildError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    cache: dict[str, dict] = {}
    if CACHE.exists() and not arguments.refresh:
        cache = json.loads(CACHE.read_text(encoding="utf-8"))

    sentences = examples()
    words = corpus_entries()
    for entry in words:
        entry["example"] = sentences.get(entry_key(entry), "")

    conjugated = {entry.get("bezokolicznik") for entry in json.loads(CONJUGATIONS.read_text(encoding="utf-8"))}
    nouns = [e for e in words if e["pos"] == "n"]
    adjectives = [e for e in words if e["pos"] == "adj"]
    verbs = [e for e in words if e["pos"] == "v" and e["word"] not in conjugated]
    pending = lambda group: [e for e in group if entry_key(e) not in cache]  # noqa: E731

    print(f"{len(nouns)} nouns, {len(adjectives)} adjectives, {len(verbs)} verbs without a conjugation")
    try:
        warm(key, pending(nouns), NOUN_PROMPT, ["gender"] + CASES, cache, "nouns")
        warm(key, pending(adjectives), ADJECTIVE_PROMPT, ["masculine", "feminine", "neuter", "pluralPersonal", "pluralOther"], cache, "adjectives")
        warm(key, pending(verbs), VERB_PROMPT, PERSONS, cache, "verbs")
    except BuildError as error:
        save(cache)
        print(f"error: {error}", file=sys.stderr)
        return 1

    save(cache)
    write_grammar(words, cache)
    write_conjugations(verbs, cache, sentences)

    missing = [e for e in nouns + adjectives + verbs if entry_key(e) not in cache]
    if missing:
        print(f"{len(missing)} words got no reply; run again to fill them in")
    return 0


if __name__ == "__main__":
    sys.exit(main())
