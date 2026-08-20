#!/usr/bin/env python3
"""Turns answer-key pages into exercises the app can run and mark.

Three shapes are recognised, chosen because between them they cover listening,
choosing and typing, and because every one of them can be marked:

    repeat        "Proszę powtórzyć"        play and read along; nothing to mark
    minimal-pair  "Co mówi lektor?"         play, pick one of two
    gap-fill      "posłuchać i uzupełnić"   play, type each gap

Everything is read from the answer key rather than the coursebook: the key prints
one exercise per page in a single clean column, where the coursebook interleaves
three exercises with an alphabet table and decorative speech bubbles. The one
thing the key cannot give is where the blanks are — it prints them filled in — so
a gap-fill also reads the book's gapped lines and aligns them with the key's
transcript.

    python3 tools/course/extract_exercises.py
"""

from __future__ import annotations

import argparse
import json
import re
import sys

from krok_paths import CACHE_DIR, pages

ANSWER_KEY = CACHE_DIR / "answer_key.json"
EXERCISES = CACHE_DIR / "exercises.json"

REPEAT = "repeat"
MINIMAL_PAIR = "minimal_pair"
GAP_FILL = "gap_fill"

# tesseract reads the list label "l)" as "|)", and loses the space in "r) sen".
LABEL_FIXES = {"|": "l"}

# tesseract loses the bracket in "r) sen", running it into the previous item as
# "rjsen/syn". Restoring it recovers two items an otherwise clean page would drop.
OCR_LABEL_DAMAGE = re.compile(r"\br\s*j\s*(?=\w)")
ITEM = re.compile(r"([a-z|])\s*[)\]]\s*([^/]+?)\s*/\s*([^ ]+(?: [^ ]+)??)(?=\s+[a-z|]\s*[)\]]|$)")
TRANSCRIPT_ITEM = re.compile(r"^([a-z|])\s*[)j]\s*(.+)$")


def clean(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip(" .")


def collapse(text: str) -> str:
    """Whitespace only — dot runs are the gaps and must survive."""
    return re.sub(r"[ \t]+", " ", text).strip()


def normalise_label(label: str) -> str:
    return LABEL_FIXES.get(label, label)


def classify(instruction: str) -> str | None:
    lowered = instruction.lower()
    if "co mówi lektor" in lowered:
        return MINIMAL_PAIR
    if "uzupe" in lowered and "słucha" in lowered:
        return GAP_FILL
    if "powtórz" in lowered or "powtorz" in lowered:
        return REPEAT
    return None


def parse_minimal_pair(record: dict) -> dict | None:
    """Options from the body, the spoken word from the transcript."""
    options: dict[str, list[str]] = {}
    for line in record["body"]:
        line = OCR_LABEL_DAMAGE.sub("r) ", line)
        for label, first, second in ITEM.findall(line):
            label = normalise_label(label)
            if label not in options:
                options[label] = [clean(first), clean(second)]

    answers: dict[str, str] = {}
    for line in record["transcript"]:
        match = TRANSCRIPT_ITEM.match(line)
        if match:
            answers[normalise_label(match.group(1))] = clean(match.group(2))

    items = []
    for label in sorted(options):
        choices, answer = options[label], answers.get(label)
        # An answer that is not one of the two printed options means the OCR read
        # one of them wrong; shipping it would mark a correct choice wrong.
        if answer is None or answer not in choices:
            continue
        items.append({"label": label, "options": choices, "answer": answer})

    if not items:
        return None
    return {"type": MINIMAL_PAIR, "items": items}


def parse_repeat(record: dict) -> dict | None:
    """The words to say back, as printed."""
    words = []
    for line in record["body"]:
        if len(line) < 3 or line.count(" ") > 24:
            continue
        words += [clean(w) for w in line.split(",") if clean(w)]
    words = [w for w in words if w and not re.fullmatch(r"[^\w]+", w)]
    if not words:
        return None
    return {"type": REPEAT, "items": [{"text": w} for w in words]}


GUTTER = 68
SPEAKER_PREFIX = re.compile(r"^[A-ZŚŻŹĆĄĘŁÓŃ][\w ]{2,20}?\s*:\s*")
SPEAKER_LINE = re.compile(r"^\s*([A-ZŚŻŹĆĄĘŁÓŃ][\w ]{2,20}?)\s*:\s{1,}(\S.*)$")
GAP = re.compile(r"\.{4,}|…+")


def coursebook_gapped_lines(tag: str) -> list[str]:
    """The exercise as the book prints it: dialogue lines with dot runs for blanks.

    The key prints those blanks filled in, so this is the only place the shape of
    the exercise survives. The book puts two exercises side by side, so lines are
    cut at the gutter — the column the tag itself sits in.
    """
    for page in pages("a1_coursebook"):
        lines = page.split("\n")
        header = next((i for i, l in enumerate(lines) if tag in l), None)
        if header is None:
            continue

        # The page runs two exercises side by side; keep only the tag's own column.
        start = lines[header].index(tag)
        left, right = (0, GUTTER) if start < GUTTER else (GUTTER, None)

        collected: list[str] = []
        blanks = 0
        for line in lines[header + 1 :]:
            segment = line[left:right]
            if not segment.strip():
                blanks += 1
                # A dialogue wraps across physical lines, so only a real break ends it.
                if collected and blanks > 2:
                    break
                continue
            blanks = 0
            if "wiczenie" in segment:
                break

            match = SPEAKER_LINE.match(segment)
            if match:
                collected.append(collapse(match.group(2)))
            elif collected:
                # A continuation of the line above: the book wraps mid-sentence.
                collected[-1] += " " + collapse(segment)
        if collected:
            return collected
    return []


def align_gaps(gapped: str, complete: str) -> tuple[str, list[str]] | None:
    """Match a gapped line against the transcript to find what each blank hides.

    "Dzień ..... dobry. ..... się Mami" against "Dzień dobry. Nazywam się Mami"
    gives the prompt "Dzień dobry. ___ się Mami" and the answer ["Nazywam"].

    A blank that turns out to hide nothing is the book's worked example — the
    first answer is printed in blue on the dotted line — so it folds back into
    the prompt rather than becoming a blank the learner has to guess.

    Returns None when a printed fragment is missing from the transcript, which
    means the two are not the same sentence and no answer can be trusted.
    """
    literals = [collapse(part) for part in GAP.split(gapped)]
    if len(literals) < 2:
        return None

    prompt, answers, cursor = "", [], 0
    for index, literal in enumerate(literals):
        if index > 0:
            end = complete.find(literal, cursor) if literal else len(complete)
            if literal and end < 0:
                return None
            hidden = collapse(complete[cursor:end])
            if hidden:
                answers.append(hidden)
                prompt += " ___ "
            else:
                prompt += " "
            cursor = end
        if literal:
            found = complete.find(literal, cursor if index else 0)
            if found < 0:
                return None
            prompt += literal
            cursor = found + len(literal)

    return (collapse(prompt), answers) if answers else None


def parse_gap_fill(record: dict) -> dict | None:
    gapped = coursebook_gapped_lines(record["tag"])
    if not gapped:
        return None

    # collapse, not clean: the gapped line keeps its punctuation and the two are
    # compared literally, so stripping a full stop here loses the match.
    transcript = [
        collapse(SPEAKER_PREFIX.sub("", line)).lstrip("_- ") for line in record["transcript"]
    ]

    items = []
    for line in gapped:
        if not GAP.search(line):
            continue
        for complete in transcript:
            aligned = align_gaps(line, complete)
            if aligned:
                prompt, answers = aligned
                items.append({"prompt": prompt, "answers": answers})
                break

    if not items:
        return None
    return {"type": GAP_FILL, "items": items}


def build(records: list[dict]) -> list[dict]:
    exercises = []
    for record in records:
        kind = classify(record["instruction"])
        if kind == MINIMAL_PAIR:
            parsed = parse_minimal_pair(record)
        elif kind == GAP_FILL:
            parsed = parse_gap_fill(record)
        elif kind == REPEAT:
            parsed = parse_repeat(record)
        else:
            continue

        if parsed is None:
            continue
        exercises.append(
            {
                "tag": record["tag"],
                "lesson": int(record["tag"][1:3]),
                "instruction": record["instruction"],
                **parsed,
            }
        )
    return exercises


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--show", help="print one exercise by audio tag")
    args = parser.parse_args()

    if not ANSWER_KEY.exists():
        sys.exit(f"missing {ANSWER_KEY}; run parse_answer_key.py first")
    records = json.loads(ANSWER_KEY.read_text(encoding="utf-8"))

    exercises = build(records)
    if args.show:
        match = next((e for e in exercises if e["tag"] == args.show), None)
        print(json.dumps(match, ensure_ascii=False, indent=2))
        return 0

    EXERCISES.write_text(json.dumps(exercises, ensure_ascii=False, indent=2), encoding="utf-8")
    by_type: dict[str, int] = {}
    for exercise in exercises:
        by_type[exercise["type"]] = by_type.get(exercise["type"], 0) + 1
    lessons = sorted({e["lesson"] for e in exercises})
    print(f"{len(exercises)} exercises: " + ", ".join(f"{n} {t}" for t, n in sorted(by_type.items())))
    print(f"  lessons {lessons[0]}-{lessons[-1]}")
    print(f"-> {EXERCISES}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
