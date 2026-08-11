#!/usr/bin/env python3
"""Reads e-polish.eu's answer pages into one record per exercise.

Every page carries the same three things, which is what makes the coursebook's
exercises markable at all:

    cwiczenie 1
    Co mówi lektor?                                    <- instruction
    [(c) e-polish.eu … autor: Iwona Stempek] 604 | 101B4   <- audio tag
    a) krzywy / grzyby      l) zimno / ciemno          <- the exercise
    transkrypty                                        <- the answers, as plain text
    a) krzywy
    b) cześć

The audio tag is the join key: 101B4 is lesson 01, section B, task 4, which is
also the name of the recording and how the coursebook labels the exercise.

The `transkrypty` block is why answers do not have to be read off underlining or
tick marks — it restates them in plain text. For a gap-fill it restates the whole
dialogue instead, and the answer per gap comes from aligning that against the
book's gapped version (see extract_exercises.py).

    python3 tools/course/parse_answer_key.py
"""

from __future__ import annotations

import argparse
import json
import re
import sys

from krok_paths import CACHE_DIR, pages

BOOK = "a1_answers"
MANIFEST = CACHE_DIR / "answer_key.json"

# "…autor: Iwona Stempek] 604 | 101B4" — the trailing token is the audio tag, and a
# few carry a variant suffix (102A1-a) where one recording covers two exercises.
AUDIO_TAG = re.compile(r"\|\s*([12]\d{2}[A-H]\d{1,2})(-[\w-]+)?\b")
EXERCISE_HEADING = re.compile(r"^[ćc]wiczenie\s*(\d+)", re.I)
TRANSCRIPT_HEADING = re.compile(r"^\s*transkrypty\s*$", re.I)
PAGE_FOOTER = re.compile(r"^\s*strona\s*\d+\s*$|e-polish\.eu|kurs polskiego online", re.I)

# tesseract reads the list label "l)" as "|)" and drops the space in "r) sen".
LABEL_FIXES = {"|": "l", "j": "r"}


def clean(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def parse_page(page: str) -> dict | None:
    lines = [l.rstrip() for l in page.split("\n")]

    tag_match = next((AUDIO_TAG.search(l) for l in lines if AUDIO_TAG.search(l)), None)
    if tag_match is None:
        return None

    number = next(
        (int(m.group(1)) for m in (EXERCISE_HEADING.match(l.strip()) for l in lines) if m),
        None,
    )

    # The instruction is the line under the "ćwiczenie N" heading, before the credit.
    instruction = ""
    for index, line in enumerate(lines):
        if EXERCISE_HEADING.match(line.strip()):
            for candidate in lines[index + 1 :]:
                if candidate.strip() and "e-polish.eu" not in candidate:
                    instruction = clean(candidate)
                    break
            break

    split = next((i for i, l in enumerate(lines) if TRANSCRIPT_HEADING.match(l)), len(lines))
    body = [clean(l) for l in lines[:split] if l.strip() and not PAGE_FOOTER.search(l)]
    transcript = [clean(l) for l in lines[split + 1 :] if l.strip() and not PAGE_FOOTER.search(l)]

    # Drop the header and credit lines from the body; they are not the exercise.
    body = [
        l
        for l in body
        if not EXERCISE_HEADING.match(l) and l != instruction and not AUDIO_TAG.search(l)
    ]

    return {
        "tag": tag_match.group(1),
        "variant": (tag_match.group(2) or "").lstrip("-") or None,
        "exerciseNumber": number,
        "instruction": instruction,
        "body": body,
        "transcript": transcript,
    }


def labelled_items(lines: list[str]) -> dict[str, str]:
    """Items like "a) krzywy" from a transcript block, one label to one answer.

    Two-column pages put two items on a line, so every match on the line counts.
    """
    items: dict[str, str] = {}
    for line in lines:
        for label, value in re.findall(r"([a-z|])\)?\s*\)?\s*([^)]+?)(?=\s+[a-z|]\)|$)", line):
            label = LABEL_FIXES.get(label, label)
            value = clean(value)
            if value and label not in items:
                items[label] = value
    return items


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--show", help="print one exercise by audio tag")
    args = parser.parse_args()

    book_pages = pages(BOOK)
    records, unkeyed = [], 0
    for page in book_pages:
        if not page.strip():
            continue
        record = parse_page(page)
        if record is None:
            unkeyed += 1
            continue
        records.append(record)

    if not records:
        sys.exit(f"{BOOK}: no pages carried an audio tag; has the OCR run?")

    if args.show:
        match = next((r for r in records if r["tag"] == args.show), None)
        print(json.dumps(match, ensure_ascii=False, indent=2))
        return 0

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")

    lessons = sorted({int(r["tag"][1:3]) for r in records})
    with_transcript = sum(1 for r in records if r["transcript"])
    print(f"{len(records)} exercises keyed, {unkeyed} pages without a tag")
    print(f"  lessons {lessons[0]}-{lessons[-1]}, {with_transcript} with a transcript")
    print(f"-> {MANIFEST}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
