#!/usr/bin/env python3
"""Writes the grammar every shipped word needs, from SGJP first and OpenAI only for the rest.

Run from anywhere:  python3 tools/vocabulary/build_grammar.py

Reads
    tools/vocabulary/.cache/sgjp-*.tab.gz   the Grammatical Dictionary of Polish, see sgjp.py
    tools/vocabulary/.cache/polimorf-*.tab.gz   PoliMorf, for verbs SGJP does not list
    tools/vocabulary/unlisted_verbs.tsv     hand-written forms for the few verbs neither lists
    local.properties                        openai.apiKey, only when SGJP leaves words open
    tools/vocabulary/corpus/**/*.tsv        the words and their part of speech
    tools/vocabulary/examples.tsv           each word's example sentence, as context
    data/src/androidMain/assets/conjugations.json

Writes
    tools/vocabulary/grammar.tsv            word, gloss, gender, declension, adjective forms
    data/src/androidMain/assets/conjugations.json   present tense and aspect of every verb

Then run build_assets.py to fold grammar.tsv into vocabulary_pl.json.

SGJP is the reference grammar of Polish, maintained at the Polish Academy of Sciences:
it knows that mąż is masculine personal, that stół gives stołowi and nazywać gives
nazywają, and whether a verb is perfective. An earlier version of this script asked
OpenAI for all of it and shipped hundreds of wrong forms, and left a third of the verbs
without any conjugation. OpenAI now answers only for what SGJP does not cover: words it
does not list, homonyms it cannot tell apart without a meaning, and nouns that have no
plural in normal use, which SGJP does not mark.

The conjugation trainer's list is pruned on the way: a verb SGJP marks vulgar, or built on
a vulgar root, is dropped, and so is a verb no dictionary lists unless it is a word of
the corpus or written out in unlisted_verbs.tsv. The list had grown invented and
misspelt verbs (abakować, biegnąć), which a learner would only memorise wrongly.

Answers are cached in tools/vocabulary/.grammar-cache.json. Pass --refresh to ask again.
"""

from __future__ import annotations

import argparse
import json
import re
import sys

import sgjp
from build_examples import BATCH, CONJUGATIONS, TOOLS, BuildError, api_key, corpus_entries, entry_key, entry_lines, read_tsv, request

GRAMMAR = TOOLS / "grammar.tsv"
CACHE = TOOLS / ".grammar-cache.json"
UNLISTED = TOOLS / "unlisted_verbs.tsv"

GENDERS = {"masculine personal", "masculine animate", "masculine inanimate", "feminine", "neuter", "plural only"}

CASES = list(sgjp.CASES)

PERSONS = list(sgjp.PERSONS)

ADJECTIVE_FIELDS = ["masculine", "feminine", "neuter", "pluralPersonal", "pluralOther"]

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
        print(f"  {label}: nothing to ask OpenAI")
        return

    for start in range(0, len(pending), BATCH):
        batch = pending[start : start + BATCH]
        written = ask(key, batch, prompt)
        for entry in batch:
            reply = written.get(entry_key(entry))
            if isinstance(reply, dict):
                cache[entry_key(entry)] = cleaned(reply, fields)
        print(f"  {label}: {min(start + BATCH, len(pending))}/{len(pending)} asked of OpenAI", end="\r", flush=True)
        save(cache)
    print()


def save(cache: dict) -> None:
    CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=0, sort_keys=True), encoding="utf-8")


def cached_cases(answer: dict | None) -> dict[str, tuple[str, str]]:
    if not answer:
        return {}
    cases = {}
    for case in CASES:
        singular, _, plural = answer.get(case, "").partition("/")
        cases[case] = (singular.strip(), plural.strip())
    return cases


def ambiguous(candidates: list[sgjp.Lexeme]) -> bool:
    plain = [lexeme for lexeme in candidates if lexeme.part_of_speech == "n" and not lexeme.qualifiers]
    shapes = {(lexeme.gender, tuple(map(tuple, sgjp.declension(lexeme)["nominative"]))) for lexeme in plain}
    return len(shapes) > 1


def noun_from_sgjp(word: str, lexicon: dict, cached: dict | None) -> dict | None:
    candidates = [lexeme for lexeme in lexicon.get(word, []) if lexeme.part_of_speech == "n"]
    if not candidates:
        return None
    ours = cached_cases(cached)
    if ours:
        gender = cached.get("gender", "")
        lexeme = sgjp.closest(
            candidates,
            "n",
            lambda candidate: sgjp.noun_mismatches(candidate, ours) + ([gender] if candidate.gender != gender else []),
        )
    elif ambiguous(candidates):
        return None
    else:
        lexeme = sgjp.best(candidates, "n")
    if lexeme is None or lexeme.gender is None:
        return None

    singular_only = bool(ours) and not any(plural for _, plural in ours.values())
    answer = {"gender": lexeme.gender}
    for case, (singular, plural) in sgjp.declension(lexeme).items():
        first_singular = singular[0] if singular else ""
        first_plural = "" if singular_only else (plural[0] if plural else "")
        answer[case] = f"{first_singular}/{first_plural}"
    if not answer["nominative"].strip("/"):
        return None
    return answer


def adjective_from_sgjp(word: str, lexicon: dict) -> dict | None:
    lexeme = sgjp.best(lexicon.get(word, []), "adj")
    if lexeme is None:
        return None
    forms = sgjp.adjective_forms(lexeme)
    answer = {name: (forms[name][0] if forms[name] else "") for name in ADJECTIVE_FIELDS}
    return answer if all(answer.values()) else None


def verb_from_sgjp(infinitive: str, lexicon: dict, existing: dict | None) -> tuple[dict, str | None] | None:
    lemma, before, after = sgjp.verb_lemma(infinitive)
    candidates = [lexeme for lexeme in lexicon.get(lemma, []) if lexeme.part_of_speech == "v"]
    if not candidates:
        return None
    mine = {person: sgjp.variants((existing or {}).get(person) or "") for person in PERSONS}
    if any(mine.values()):
        lexeme = sgjp.closest(
            candidates,
            "v",
            lambda candidate: [
                person
                for person, forms in sgjp.present_tense(candidate).items()
                if mine[person] and not set(mine[person]) <= {before + form + after for form in forms}
            ],
        )
    else:
        lexeme = sgjp.best(candidates, "v")
    if lexeme is None:
        return None
    tense = sgjp.present_tense(lexeme)
    if not any(tense.values()):
        return None
    forms = {person: "; ".join(before + form + after for form in tense[person]) for person in PERSONS}
    return forms, lexeme.aspect


def verbs_in(lexicon: dict, infinitive: str) -> list[sgjp.Lexeme]:
    return [lexeme for lexeme in lexicon.get(sgjp.verb_lemma(infinitive)[0], []) if lexeme.part_of_speech == "v"]


def vulgar(infinitive: str, candidates: list[sgjp.Lexeme]) -> bool:
    if sgjp.VULGAR_ROOT.search(infinitive):
        return True
    return bool(candidates) and all("wulg" in lexeme.qualifiers for lexeme in candidates)


def unlisted_verbs() -> dict[str, tuple[dict, str]]:
    return {
        cols[0]: ({person: cols[2 + index] for index, person in enumerate(PERSONS)}, cols[1])
        for cols in read_tsv(UNLISTED)
        if len(cols) >= 2 + len(PERSONS)
    }


def write_grammar(lines_in: list[tuple[dict, dict]]) -> None:
    lines = [
        "# Generated by build_grammar.py from SGJP, with OpenAI for what SGJP lacks. Columns: polish <TAB> english "
        "<TAB> gender <TAB> declension (seven cases, ; between cases, singular|plural) <TAB> masculine <TAB> "
        "feminine <TAB> neuter <TAB> plural personal <TAB> plural other."
    ]
    for entry, answer in lines_in:
        if entry["pos"] == "n":
            cases = ";".join(answer.get(case, "").replace("/", "|") for case in CASES)
            lines.append(f"{entry['word']}\t{entry['gloss']}\t{answer['gender']}\t{cases}\t\t\t\t\t")
        else:
            forms = [answer[name] for name in ADJECTIVE_FIELDS]
            lines.append(f"{entry['word']}\t{entry['gloss']}\t\t\t" + "\t".join(forms))
    GRAMMAR.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh", action="store_true", help="ignore the cache and ask OpenAI again")
    arguments = parser.parse_args()

    cache: dict[str, dict] = {}
    if CACHE.exists() and not arguments.refresh:
        cache = json.loads(CACHE.read_text(encoding="utf-8"))

    sentences = examples()
    words = corpus_entries()
    for entry in words:
        entry["example"] = sentences.get(entry_key(entry), "")

    conjugations = json.loads(CONJUGATIONS.read_text(encoding="utf-8"))
    by_infinitive = {verb.get("bezokolicznik"): verb for verb in conjugations}
    nouns = [e for e in words if e["pos"] == "n"]
    adjectives = [e for e in words if e["pos"] == "adj"]
    new_verbs = [e for e in words if e["pos"] == "v" and e["word"] not in by_infinitive]

    lemmas = {e["word"] for e in nouns + adjectives}
    lemmas |= {sgjp.verb_lemma(infinitive)[0] for infinitive in by_infinitive if infinitive}
    lemmas |= {sgjp.verb_lemma(e["word"])[0] for e in new_verbs}
    try:
        lexicon = sgjp.load(lemmas)
    except FileNotFoundError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    from_sgjp = {
        entry_key(e): answer
        for e in nouns
        if (answer := noun_from_sgjp(e["word"], lexicon, cache.get(entry_key(e)))) is not None
    }
    from_sgjp |= {
        entry_key(e): answer for e in adjectives if (answer := adjective_from_sgjp(e["word"], lexicon)) is not None
    }

    open_nouns = [e for e in nouns if entry_key(e) not in from_sgjp and entry_key(e) not in cache]
    open_adjectives = [e for e in adjectives if entry_key(e) not in from_sgjp and entry_key(e) not in cache]
    open_verbs = [
        e for e in new_verbs if verb_from_sgjp(e["word"], lexicon, None) is None and entry_key(e) not in cache
    ]
    print(f"{len(nouns)} nouns, {len(adjectives)} adjectives, {len(conjugations)} verbs, {len(new_verbs)} new verbs")
    print(f"  SGJP answers {len(from_sgjp)} nouns and adjectives; OpenAI is needed for {len(open_nouns) + len(open_adjectives) + len(open_verbs)}")

    if open_nouns or open_adjectives or open_verbs:
        try:
            key = api_key()
            warm(key, open_nouns, NOUN_PROMPT, ["gender"] + CASES, cache, "nouns")
            warm(key, open_adjectives, ADJECTIVE_PROMPT, ADJECTIVE_FIELDS, cache, "adjectives")
            warm(key, open_verbs, VERB_PROMPT, PERSONS, cache, "verbs")
        except BuildError as error:
            save(cache)
            print(f"warning: {error}; those words keep no grammar until this runs again", file=sys.stderr)
        save(cache)

    rows: list[tuple[dict, dict]] = []
    sources = {"SGJP": 0, "OpenAI": 0}
    for entry in nouns + adjectives:
        answer = from_sgjp.get(entry_key(entry))
        source = "SGJP"
        if answer is None:
            answer = cache.get(entry_key(entry))
            source = "OpenAI"
            if entry["pos"] == "n" and noun_from_sgjp(entry["word"], lexicon, answer) is not None:
                answer = noun_from_sgjp(entry["word"], lexicon, answer)
                source = "SGJP"
        if not answer:
            continue
        if entry["pos"] == "n" and not usable_noun(answer):
            continue
        if entry["pos"] == "adj" and not answer.get("masculine"):
            continue
        rows.append((entry, answer))
        sources[source] += 1
    write_grammar(rows)
    print(f"grammar.tsv: {len(rows)} words, {sources['SGJP']} from SGJP, {sources['OpenAI']} from OpenAI")

    unknown = {sgjp.verb_lemma(i)[0] for i in by_infinitive if i and not verbs_in(lexicon, i)}
    polimorf = sgjp.load(unknown, "polimorf")
    unlisted = unlisted_verbs()
    corpus_verbs = {e["word"] for e in words if e["pos"] == "v"}
    verb_sources = {"SGJP": 0, "PoliMorf": 0, "hand-written": 0, "kept": 0, "added": 0}
    dropped: dict[str, list[str]] = {"vulgar": [], "in no dictionary": []}
    kept_verbs = []
    for verb in conjugations:
        infinitive = verb.get("bezokolicznik", "")
        if not infinitive:
            continue
        if vulgar(infinitive, verbs_in(lexicon, infinitive) or verbs_in(polimorf, infinitive)):
            dropped["vulgar"].append(infinitive)
            continue
        if infinitive in unlisted:
            found, source = unlisted[infinitive], "hand-written"
        elif (found := verb_from_sgjp(infinitive, lexicon, verb)) is not None:
            source = "SGJP"
        elif (found := verb_from_sgjp(infinitive, polimorf, verb)) is not None:
            source = "PoliMorf"
        elif infinitive in corpus_verbs:
            verb.pop("aspect", None)
            verb_sources["kept"] += 1
            kept_verbs.append(verb)
            continue
        else:
            dropped["in no dictionary"].append(infinitive)
            continue
        forms, aspect = found
        verb.update(forms)
        if aspect:
            verb["aspect"] = aspect
        else:
            verb.pop("aspect", None)
        verb_sources[source] += 1
        kept_verbs.append(verb)
    conjugations = kept_verbs
    for entry in new_verbs:
        found = verb_from_sgjp(entry["word"], lexicon, None)
        if found is not None:
            forms, aspect = found
        else:
            answer = cache.get(entry_key(entry))
            if not answer or not any(answer.get(person) for person in PERSONS):
                continue
            forms, aspect = {person: answer.get(person, "") for person in PERSONS}, None
        added = {"bezokolicznik": entry["word"], **forms, "translation": entry["gloss"], "example": sentences.get(entry_key(entry), "")}
        if aspect:
            added["aspect"] = aspect
        conjugations.append(added)
        verb_sources["added"] += 1
    conjugations.sort(key=lambda it: it.get("bezokolicznik", ""))
    CONJUGATIONS.write_text(json.dumps(conjugations, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"conjugations.json: {len(conjugations)} verbs, " + ", ".join(f"{n} {source}" for source, n in verb_sources.items()))
    for reason, verbs in dropped.items():
        print(f"  dropped, {reason}: {len(verbs)}: {', '.join(verbs)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
