#!/usr/bin/env python3
"""Adds the everyday words and phrases the corpus lacks, found in Tatoeba's real sentences.

Run from anywhere:  python3 tools/vocabulary/build_tatoeba.py

Reads
    tools/vocabulary/.cache/                Tatoeba exports and SGJP, see tatoeba.py and sgjp.py
    local.properties                        openai.apiKey
    tools/vocabulary/corpus/**/*.tsv        the words already taught

Writes
    tools/vocabulary/corpus/topics/zzzz-tatoeba-phrases.tsv   set phrases, as corpus rows
    tools/vocabulary/corpus/topics/zzzz-tatoeba-words.tsv     new words, most frequent first
    tools/vocabulary/tatoeba_examples.tsv   a real sentence for each new word, with its translation
    tools/vocabulary/tatoeba_credits.tsv    id and author of every Tatoeba sentence used

Then run build_grammar.py, build_examples.py, build_pictures.py and build_assets.py.

The corpus was written from frequency lists of single words, so it misses what people say
to each other. Tatoeba's 78,000 Polish sentences with English translations are that
speech. SGJP turns each sentence into dictionary forms, so a word's frequency is the
number of sentences that use it in any form, and kupić counts for kupię, kupiła and
kupiony. The words used in at least MIN_SENTENCES sentences and missing from the corpus
are candidates; OpenAI then turns away names and stray forms, gives each the meaning those
sentences use, a level and topics, and picks the sentence that shows the meaning best.

A candidate is dropped when one of its forms is already a headword: the app teaches
pieniądze and wszystko, so pieniądz and wszystek would only repeat them. The chosen
sentence becomes the new word's example, so a new word ships with a sentence a person
wrote rather than one a model made up. Words and trainer verbs already taught get the
same: where a short Tatoeba sentence uses them in their meaning, it replaces the example
OpenAI wrote, which was sometimes odd (Zimny wiatr dygotał miastem). A sentence is used
only when SGJP knows every word in it: Tatoeba's contributors make typos too (podróźy,
jeśle), and a sentence opening with a name (Yanni, Ziri) says little about the word.

Phrases come from short sentences whose words recur, in that order, inside at least
PHRASE_RECURRENCE other sentences: nie wiem, mam nadzieję and w porządku do, light the
candle does not. That is what makes a phrase set rather than built. OpenAI keeps those that are said as a whole
(Nie ma sprawy, Smacznego) and leaves ordinary sentences built from words.

The new files sort after every existing corpus file, so no shipped word changes its id.
Answers are cached in tools/vocabulary/.tatoeba-cache.json. Pass --refresh to ask again.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed

import sgjp
import tatoeba
from build_examples import BATCH, CONJUGATIONS, TOOLS, BuildError, api_key, read_tsv, request

CACHE = TOOLS / ".tatoeba-cache.json"
WORDS = TOOLS / "corpus" / "topics" / "zzzz-tatoeba-words.tsv"
PHRASES = TOOLS / "corpus" / "topics" / "zzzz-tatoeba-phrases.tsv"
EXAMPLES = TOOLS / "tatoeba_examples.tsv"
CREDITS = TOOLS / "tatoeba_credits.tsv"

MIN_SENTENCES = 8
CONTEXT_SENTENCES = 4
MIN_WORD_LENGTH = 3
MAX_PHRASE_WORDS = 4
PHRASE_RECURRENCE = 8
MIN_TOPIC_USES = 10
PHRASE_BATCH = 100
MAX_EXAMPLE_WORDS = 10
EXAMPLE_BATCH = 50
PARALLEL_REQUESTS = 8

POS_TAGS = {"n", "v", "adj", "adv", "prn", "num", "prep", "conj", "part", "interj"}
CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"}

WORD_PROMPT = """Below are Polish words a learner's vocabulary app does not teach yet, each with
what the Grammatical Dictionary of Polish says it can be and real sentences that use it.

For each, decide whether the app should teach it, and describe it.

Rules:
* keep is false for names and words standing for a name in these sentences (Tom, Mary),
  for a form that is really another word (jaka is a form of jaki), for abbreviations, and
  for anything offensive. Otherwise keep is true.
* gloss is the English meaning these sentences use, as a dictionary gives it, one to four
  words, lowercase: "to buy", "money", "favourite", "still".
* pos is one of: n, v, adj, adv, prn, num, prep, conj, part, interj.
* cefr is the level at which a learner usually meets the word, A1 to C2.
* topics are up to two of these, only if they fit: {{topics}}
* sentence is the number of the sentence that shows this meaning best and is easiest for
  a learner, or 0 when none of them uses the word in that meaning.

Return ONLY a JSON object mapping each entry number, as a string, to
{"keep": true, "gloss": "...", "pos": "v", "cefr": "A2", "topics": ["shopping"], "sentence": 2}.

Entries:

{{entries}}
"""

EXAMPLE_PROMPT = """Each Polish entry below has its meaning and real sentences that use it.

For each, pick the sentence that uses the entry in exactly that meaning, reads naturally,
and suits a learner at the level of the entry. Answer 0 when no sentence uses it in that
meaning, or when every one is odd, gloomy, or about a named person.

Return ONLY a JSON object mapping each entry number, as a string, to the number of the
chosen sentence as a string, or "0": {"1": "2", "2": "0"}.

Entries:

{{entries}}
"""

PHRASE_PROMPT = """Below are short Polish sentences with their English translations.

Be selective. Pick only the ones a learner should learn as a whole because people say
them as set phrases, the way a phrasebook lists them:
greetings, thanks and apologies, reactions, polite requests, and stock questions and
answers, such as "Nie ma sprawy.", "Smacznego!", "Ile to kosztuje?", "Wszystko w porządku?".
Leave out ordinary sentences built from words ("Tom jest wysoki."), sentences about a
particular person or thing, and near-duplicates of a phrase you already picked.

For each phrase you pick give a short natural English gloss in lowercase without a final
full stop, the CEFR level at which a learner needs it (A1 to C2), and up to two of these
topics if they fit: {{topics}}

Return ONLY a JSON object mapping the number of each picked sentence, as a string, to
{"gloss": "...", "cefr": "A1", "topics": ["greetings"]}. Leave the others out.

Sentences:

{{entries}}
"""


def topic_names(words: list[dict]) -> list[str]:
    uses = Counter(topic.strip() for entry in words for topic in entry["topics"].split(",") if topic.strip())
    return sorted(topic for topic, count in uses.items() if count >= MIN_TOPIC_USES)


def normalised(text: str) -> str:
    return " ".join(word.lower() for word in tatoeba.TOKEN.findall(text))


def phrase_text(sentence: str) -> str:
    text = re.sub(r"[\s.!?…]+$", "", sentence.strip())
    return text[:1].lower() + text[1:]


def contexts(lemma: str, by_lemma: dict[str, list[tatoeba.Pair]], reflexive: bool = False) -> list[tatoeba.Pair]:
    found = sorted(
        (pair for pair in by_lemma.get(lemma, []) if not reflexive or "się" in tatoeba.words_of(pair.polish)),
        key=lambda pair: (abs(len(pair.tokens) - 6), pair.id),
    )
    chosen, seen = [], set()
    for pair in found:
        if normalised(pair.polish) not in seen:
            chosen.append(pair)
            seen.add(normalised(pair.polish))
        if len(chosen) == CONTEXT_SENTENCES:
            break
    return chosen


def word_lines(batch: list[dict]) -> str:
    lines = []
    for number, entry in enumerate(batch, start=1):
        lines.append(f"{number}. {entry['word']} (dictionary: {', '.join(entry['kinds']) or 'unknown'})")
        for index, pair in enumerate(entry["contexts"], start=1):
            lines.append(f"   {index}) {pair.polish} = {pair.english}")
    return "\n".join(lines)


def ask_words(key: str, batch: list[dict], topics: list[str]) -> dict[str, dict]:
    prompt = WORD_PROMPT.replace("{{topics}}", ", ".join(topics)).replace("{{entries}}", word_lines(batch))
    answer = request(key, prompt)
    return {
        batch[int(n) - 1]["word"]: reply
        for n, reply in answer.items()
        if isinstance(reply, dict) and n.isdigit() and 0 < int(n) <= len(batch)
    }


def ask_examples(key: str, batch: list[dict]) -> dict[str, int]:
    return {entry: int(choice) for entry, choice in ask_example_choices(key, batch).items() if str(choice).isdigit()}


def ask_example_choices(key: str, batch: list[dict]) -> dict[str, str]:
    lines = []
    for number, entry in enumerate(batch, start=1):
        lines.append(f"{number}. {entry['word']} = {entry['gloss']} ({entry['level']})")
        for index, pair in enumerate(entry["contexts"], start=1):
            lines.append(f"   {index}) {pair.polish} = {pair.english}")
    answer = request(key, EXAMPLE_PROMPT.replace("{{entries}}", "\n".join(lines)))
    return {
        f"{batch[int(n) - 1]['word']}\t{batch[int(n) - 1]['gloss']}": reply
        for n, reply in answer.items()
        if n.isdigit() and 0 < int(n) <= len(batch)
    }


def ask_phrases(key: str, batch: list[tatoeba.Pair], topics: list[str]) -> dict[str, dict]:
    entries = "\n".join(f"{n}. {pair.polish} = {pair.english}" for n, pair in enumerate(batch, start=1))
    prompt = PHRASE_PROMPT.replace("{{topics}}", ", ".join(topics)).replace("{{entries}}", entries)
    answer = request(key, prompt)
    return {
        batch[int(n) - 1].id: reply
        for n, reply in answer.items()
        if isinstance(reply, dict) and n.isdigit() and 0 < int(n) <= len(batch)
    }


def save(cache: dict) -> None:
    CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=0, sort_keys=True), encoding="utf-8")


def clean_topics(reply: dict, allowed: set[str]) -> str:
    topics = reply.get("topics") or []
    return ",".join(dict.fromkeys(t for t in topics if isinstance(t, str) and t in allowed))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh", action="store_true", help="ignore the cache and ask again for everything")
    parser.add_argument("--dry-run", action="store_true", help="count the candidates without asking OpenAI")
    arguments = parser.parse_args()

    cache: dict[str, dict] = {}
    if CACHE.exists() and not arguments.refresh:
        cache = json.loads(CACHE.read_text(encoding="utf-8"))

    shipped_files = {WORDS.name, PHRASES.name}
    corpus = [
        e
        for path in [TOOLS / "corpus" / "core.tsv", *sorted((TOOLS / "corpus" / "topics").glob("*.tsv"))]
        if path.name not in shipped_files
        for e in [
            {"word": c[0].strip(), "gloss": c[1].strip(), "pos": c[2].strip(), "level": c[3].strip(), "topics": c[4].strip() if len(c) > 4 else ""}
            for c in read_tsv(path)
            if len(c) >= 4
        ]
    ]
    taught = {e["word"].lower() for e in corpus}
    taught_phrases = {normalised(e["word"]) for e in corpus}
    taught_pairs = {(e["word"].lower(), e["gloss"].lower()) for e in corpus}
    topics = topic_names(corpus)
    allowed_topics = set(topics)

    try:
        sentences = tatoeba.pairs()
        lemmatiser = tatoeba.Lemmatiser(sentences)
    except FileNotFoundError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    lemmatiser.annotate(sentences)
    counts = tatoeba.frequency(sentences)
    by_lemma: dict[str, list[tatoeba.Pair]] = {}
    for pair in sentences:
        for lemma in set(filter(None, pair.lemmas)):
            by_lemma.setdefault(lemma, []).append(pair)

    ranked = [
        lemma
        for lemma, count in counts.most_common()
        if count >= MIN_SENTENCES
        and len(lemma) >= MIN_WORD_LENGTH
        and lemma not in taught
        and lemma not in lemmatiser.vulgar
        and not sgjp.VULGAR_ROOT.search(lemma)
    ]
    lexicon = sgjp.load(set(ranked))
    candidates = [
        {
            "word": lemma,
            "kinds": sorted({lexeme.part_of_speech for lexeme in lexicon.get(lemma, []) if lexeme.part_of_speech}),
            "contexts": contexts(lemma, by_lemma),
        }
        for lemma in ranked
    ]
    print(f"{len(sentences)} Tatoeba sentences with a translation, {len(counts)} lemmas, {len(candidates)} new candidates")

    short_pairs: dict[str, list[tatoeba.Pair]] = {}
    for lemma, found in by_lemma.items():
        fitting = [pair for pair in found if len(pair.tokens) <= MAX_EXAMPLE_WORDS]
        if fitting:
            short_pairs[lemma] = fitting
    verbs = json.loads(CONJUGATIONS.read_text(encoding="utf-8"))
    shown = [
        {"word": e["word"], "gloss": e["gloss"], "level": e.get("level", ""), "lemma": e["word"].lower(), "reflexive": False}
        for e in corpus
        if e["pos"] != "expr" and " " not in e["word"]
    ]
    for verb in verbs:
        infinitive = verb.get("bezokolicznik", "")
        lemma, before, after = sgjp.verb_lemma(infinitive)
        if infinitive and not before and after in ("", " się"):
            shown.append({"word": infinitive, "gloss": verb.get("translation", ""), "level": "B1", "lemma": lemma, "reflexive": bool(after)})
    for entry in shown:
        entry["contexts"] = contexts(entry["lemma"], short_pairs, entry["reflexive"])
    shown = [entry for entry in shown if entry["contexts"]]
    print(f"{len(shown)} taught words and verbs Tatoeba has a short sentence for")

    every_sentence = tatoeba.polish_sentences()
    recurring = tatoeba.recurrence(every_sentence, MAX_PHRASE_WORDS)
    named = tatoeba.names(every_sentence)
    short = []
    seen_phrases = set(taught_phrases)
    for pair in sorted(sentences, key=lambda p: (-recurring[tuple(normalised(p.polish).split())], p.id)):
        words = tatoeba.words_of(pair.polish)
        if not 1 < len(words) <= MAX_PHRASE_WORDS or None in words or words[0] in named:
            continue
        if recurring[tuple(words)] < PHRASE_RECURRENCE:
            continue
        if any(lemma in lemmatiser.vulgar for lemma in pair.lemmas if lemma):
            continue
        key = normalised(pair.polish)
        if key in seen_phrases:
            continue
        seen_phrases.add(key)
        short.append(pair)
    print(f"{len(short)} short sentences to look through for set phrases")
    if arguments.dry_run:
        print("  first candidates: " + ", ".join(c["word"] for c in candidates[:40]))
        return 0

    try:
        key = None
        pending_words = [c for c in candidates if f"word:{c['word']}" not in cache]
        for start in range(0, len(pending_words), BATCH):
            key = key or api_key()
            batch = pending_words[start : start + BATCH]
            replies = ask_words(key, batch, topics)
            for candidate in batch:
                cache[f"word:{candidate['word']}"] = replies.get(candidate["word"], {"keep": False, "unanswered": True})
            save(cache)
            print(f"  words: {min(start + BATCH, len(pending_words))}/{len(pending_words)}", end="\r", flush=True)
        print()
        pending_phrases = [p for p in short if f"phrase:{p.id}" not in cache]
        for start in range(0, len(pending_phrases), PHRASE_BATCH):
            key = key or api_key()
            batch = pending_phrases[start : start + PHRASE_BATCH]
            replies = ask_phrases(key, batch, topics)
            for pair in batch:
                cache[f"phrase:{pair.id}"] = replies.get(pair.id, {})
            save(cache)
            print(f"  phrases: {min(start + PHRASE_BATCH, len(pending_phrases))}/{len(pending_phrases)}", end="\r", flush=True)
        print()
        pending_examples = [e for e in shown if f"example:{e['word']}\t{e['gloss']}" not in cache]
        batches = [pending_examples[start : start + EXAMPLE_BATCH] for start in range(0, len(pending_examples), EXAMPLE_BATCH)]
        if batches:
            key = key or api_key()
        def answer(batch: list[dict]) -> dict[str, int] | None:
            try:
                return ask_examples(key, batch)
            except BuildError as error:
                print(f"\n  a batch failed and is left for the next run: {error}", file=sys.stderr)
                return None

        done = 0
        with ThreadPoolExecutor(max_workers=PARALLEL_REQUESTS) as pool:
            futures = {pool.submit(answer, batch): batch for batch in batches}
            for future in as_completed(futures):
                batch, replies = futures[future], future.result()
                if replies is None:
                    continue
                for entry in batch:
                    entry_key = f"{entry['word']}\t{entry['gloss']}"
                    cache[f"example:{entry_key}"] = {"sentence": replies.get(entry_key, 0)}
                save(cache)
                done += len(batch)
                print(f"  examples: {done}/{len(pending_examples)}", end="\r", flush=True)
        print()
    except BuildError as error:
        save(cache)
        print(f"error: {error}", file=sys.stderr)
        return 1

    credits: dict[str, tuple[str, str, str, str]] = {}
    new_words, examples = [], []
    for candidate in candidates:
        reply = cache.get(f"word:{candidate['word']}", {})
        gloss = str(reply.get("gloss", "")).strip().lower()
        pos = str(reply.get("pos", "")).strip()
        cefr = str(reply.get("cefr", "")).strip().upper()
        if not reply.get("keep") or not gloss or pos not in POS_TAGS or cefr not in CEFR_LEVELS:
            continue
        if (candidate["word"], gloss) in taught_pairs:
            continue
        forms = {form.text.lower() for lexeme in lexicon.get(candidate["word"], []) for form in lexeme.forms}
        if forms & taught:
            continue
        aspect = None
        if pos == "v":
            verb = sgjp.best(lexicon.get(candidate["word"], []), "v")
            aspect = verb.aspect if verb else None
        new_words.append(
            {"word": candidate["word"], "gloss": gloss, "pos": pos, "cefr": cefr, "topics": clean_topics(reply, allowed_topics), "aspect": aspect}
        )
        index = reply.get("sentence")
        if isinstance(index, int) and 0 < index <= len(candidate["contexts"]):
            pair = candidate["contexts"][index - 1]
            marked = pair.marked(candidate["word"]) if lemmatiser.is_clean(pair) else None
            if marked:
                examples.append((candidate["word"], gloss, marked, pair))
                credits[pair.id] = (pair.id, pair.author, pair.english_id, pair.english_author)

    glosses = Counter(e["gloss"] for e in corpus) + Counter(w["gloss"] for w in new_words)
    for word in new_words:
        if word["aspect"] == "perfective" and glosses[word["gloss"]] > 1 and "perfective" not in word["gloss"]:
            old = word["gloss"]
            word["gloss"] = f"{old} (perfective)"
            examples[:] = [(w, word["gloss"] if (w == word["word"] and g == old) else g, m, p) for w, g, m, p in examples]

    new_phrases = []
    used_glosses = {e["gloss"].lower() for e in corpus}
    for pair in short:
        reply = cache.get(f"phrase:{pair.id}", {})
        gloss = str(reply.get("gloss", "")).strip()
        cefr = str(reply.get("cefr", "")).strip().upper()
        text = phrase_text(pair.polish)
        if not gloss or cefr not in CEFR_LEVELS or len(text) < MIN_WORD_LENGTH:
            continue
        if (text.lower(), gloss.lower()) in taught_pairs or gloss.lower() in used_glosses:
            continue
        used_glosses.add(gloss.lower())
        new_phrases.append({"word": text, "gloss": gloss, "pos": "expr", "cefr": cefr, "topics": clean_topics(reply, allowed_topics)})
        credits[pair.id] = (pair.id, pair.author, pair.english_id, pair.english_author)

    replaced = 0
    for entry in shown:
        index = cache.get(f"example:{entry['word']}\t{entry['gloss']}", {}).get("sentence")
        if isinstance(index, int) and 0 < index <= len(entry["contexts"]):
            pair = entry["contexts"][index - 1]
            marked = pair.marked(entry["lemma"]) if lemmatiser.is_clean(pair) else None
            if marked:
                examples.append((entry["word"], entry["gloss"], marked, pair))
                credits[pair.id] = (pair.id, pair.author, pair.english_id, pair.english_author)
                replaced += 1
    print(f"{replaced} taught words and verbs get a Tatoeba example")

    header = "# Generated by build_tatoeba.py. Columns as in core.tsv."
    WORDS.write_text(
        header + " Words frequent in Tatoeba's sentences, most frequent first.\n"
        + "".join(f"{w['word']}\t{w['gloss']}\t{w['pos']}\t{w['cefr']}\t{w['topics']}\n" for w in new_words),
        encoding="utf-8",
    )
    PHRASES.write_text(
        header + " Set phrases from Tatoeba's short sentences.\n"
        + "".join(f"{p['word']}\t{p['gloss']}\t{p['pos']}\t{p['cefr']}\t{p['topics']}\n" for p in new_phrases),
        encoding="utf-8",
    )
    EXAMPLES.write_text(
        "# Generated by build_tatoeba.py. Columns: polish <TAB> english <TAB> sentence <TAB> its translation <TAB> Tatoeba id.\n"
        + "".join(f"{w}\t{g}\t{m}\t{p.english}\t{p.id}\n" for w, g, m, p in examples),
        encoding="utf-8",
    )
    CREDITS.write_text(
        "# Generated by build_tatoeba.py. Tatoeba sentences used, CC BY 2.0 FR: "
        "Polish id <TAB> author <TAB> English id <TAB> author. See https://tatoeba.org/sentences/show/<id>.\n"
        + "".join("\t".join(row) + "\n" for row in sorted(credits.values(), key=lambda row: int(row[0]))),
        encoding="utf-8",
    )
    print(f"{len(new_words)} new words ({len(examples)} with a Tatoeba example), {len(new_phrases)} new phrases, {len(credits)} sentences credited")
    return 0


if __name__ == "__main__":
    sys.exit(main())
