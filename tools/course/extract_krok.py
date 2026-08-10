#!/usr/bin/env python3
"""Parse Krok po kroku lesson structure out of the book text.

Every lesson opens on a page laid out the same way:

    PIERWSZY DZIEŃ
       W SZKOLE
                              Lekcja_01
    komunikacja        słownictwo         gramatyka
    powitania          podstawowe zwroty  alfabet
    przedstawianie się                    liczebniki 0-10

    tak, nie, proszę, dziękuję, ...
                                          nowe słowa

so the opener yields the title, the three syllabus columns and the new-word
list. The rest of the lesson yields the audio tags (101A1, 126C9) that tie an exercise
to a track.

pdftotext -layout keeps horizontal position, which is what makes the three
columns recoverable: the header row fixes each column's left edge, and every
later row is split on its wide gutters and matched back to those edges.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from itertools import combinations
from pathlib import Path

from krok_paths import CACHE_DIR, is_ocr_source, pages

LESSON_MARKER = re.compile(r"Lekcja[_ ](\d{1,2})")

# OCR renders the same marker as "Lekcia 02" or drops the number altogether, so the
# OCR path matches loosely and reconciles the numbering afterwards. It stays
# case-sensitive because "lekcja" is also an ordinary word: a page reading "lekcja
# języka na uniwersytecie" is not a lesson opener.
OCR_LESSON_MARKER = re.compile(r"\bLek[cs][ijl]?a\b\s*(\d{0,2})")

# The marker is printed on a line of its own, so anything much longer is prose that
# happens to contain the word.
MARKER_LINE_SLACK = 3

# A run with this many commas is a word list rather than a sentence.
MIN_COMMAS_IN_WORD_LIST = 3

# ...and every entry on it is a word or a short phrase. Prose also has commas, and a
# lesson whose opener carries no word list at all would otherwise take a paragraph.
MAX_WORDS_PER_ENTRY = 4

# tesseract renders the books' dotted fill-in rules as long runs of noise.
OCR_NOISE = re.compile(r"\.{3,}|_{3,}")
COLUMN_HEADERS = ("komunikacja", "słownictwo", "gramatyka")
NEW_WORDS_MARKER = "nowe słowa"
AUDIO_TAG = re.compile(r"\b([12])(\d{2})([A-H])(\d{1,2})\b")
PAGE_FOOTER = re.compile(r"download this book from|^\s*_?\d+\s*$")

EXPECTED_LESSONS = {"a1_coursebook": 26, "a2_coursebook": 23}

OVERRIDES = Path(__file__).parent / "lesson_overrides.tsv"

UPPERCASE_LETTERS = set("ABCDEFGHIJKLMNOPRSTUWXYZĄĆĘŁŃÓŚŹŻ")


def is_title_line(line: str) -> bool:
    text = line.strip()
    if len(text) < 3:
        return False
    letters = [c for c in text if c.isalpha()]
    if len(letters) < 3:
        return False
    return all(c in UPPERCASE_LETTERS for c in letters)


def title_from_opener(lines: list[str], marker_index: int) -> str:
    """The lesson title sits in large caps immediately above the Lekcja_NN marker."""
    collected: list[str] = []
    for line in reversed(lines[max(0, marker_index - 6) : marker_index]):
        if is_title_line(line):
            collected.append(" ".join(line.split()))
        elif collected:
            break
    return " ".join(reversed(collected))


def column_spans(header: str) -> list[tuple[str, int]] | None:
    """Left character offset of each of the komunikacja/słownictwo/gramatyka columns."""
    lowered = header.lower()
    starts = []
    for name in COLUMN_HEADERS:
        index = lowered.find(name)
        if index < 0:
            return None
        starts.append((name, index))
    starts.sort(key=lambda pair: pair[1])
    return starts


def clean(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip(" ,;")


def split_runs(line: str) -> list[tuple[int, str]]:
    """Text runs on a laid-out line with their column offsets, split on wide gutters."""
    return [(match.start(), match.group()) for match in re.finditer(r"\S(?:.*?\S)?(?=\s{3,}|$)", line)]


def assign_columns(runs: list[tuple[int, str]], columns: list[tuple[str, int]]) -> list[tuple[str, str]]:
    """Attach each run on a row to a column, keeping left-to-right order.

    Cells are not flush with their labels — a long entry can start a dozen
    characters left of the header it belongs to — so the nearest header alone
    would sometimes drop two runs into one column and leave another empty.
    Choosing the order-preserving assignment with the smallest total offset
    distance cannot do that.
    """
    if not runs:
        return []

    best = min(
        combinations(range(len(columns)), len(runs)),
        key=lambda choice: sum(abs(runs[i][0] - columns[c][1]) for i, c in enumerate(choice)),
    )
    return [(columns[c][0], runs[i][1]) for i, c in enumerate(best)]


def is_marker_line(line: str) -> bool:
    """A line that is the lesson label itself, not prose containing the word."""
    match = OCR_LESSON_MARKER.search(line)
    return match is not None and len(line.strip()) <= len(match.group()) + MARKER_LINE_SLACK


def looks_like_word_list(line: str) -> bool:
    if line.count(",") < MIN_COMMAS_IN_WORD_LIST or OCR_NOISE.search(line):
        return False
    entries = [part.strip() for part in line.split(",") if part.strip()]
    return bool(entries) and all(len(entry.split()) <= MAX_WORDS_PER_ENTRY for entry in entries)


def parse_ocr_opener(page: str) -> dict | None:
    """A lesson opener read from an OCR transcript rather than a laid-out PDF.

    tesseract does not preserve the horizontal positions that make the three
    syllabus columns recoverable, so only the number, the title and the new-word
    list are taken. The word list is what the lessons are actually built from, and
    it is one comma-separated run, which survives OCR intact.
    """
    lines = page.split("\n")
    marker = next((i for i, line in enumerate(lines) if is_marker_line(line)), None)
    if marker is None:
        return None

    number = OCR_LESSON_MARKER.search(lines[marker]).group(1)
    new_words = []
    for line in lines[marker + 1 :]:
        if NEW_WORDS_MARKER in line.lower():
            break
        if looks_like_word_list(line):
            new_words.append(clean(line.rstrip("|").strip()))

    return {
        "number": int(number) if number else 0,
        "title": title_from_opener(lines, marker),
        "communication": [],
        "vocabularyTopics": [],
        "grammar": [],
        "newWords": split_new_words(new_words),
    }


def parse_opener(page: str) -> dict | None:
    lines = page.split("\n")
    marker = next((i for i, line in enumerate(lines) if LESSON_MARKER.search(line)), None)
    if marker is None:
        return None

    header = next(
        (i for i in range(marker + 1, min(marker + 6, len(lines))) if column_spans(lines[i])),
        None,
    )
    if header is None:
        return None

    columns = column_spans(lines[header])
    syllabus: dict[str, list[str]] = {name: [] for name in COLUMN_HEADERS}
    new_words: list[str] = []
    in_new_words = False

    for line in lines[header + 1 :]:
        if NEW_WORDS_MARKER in line.lower():
            break
        if PAGE_FOOTER.search(line):
            continue
        if not line.strip():
            # A blank line closes the three-column block; whatever follows it, up
            # to the "nowe słowa" caption, is the lesson's new-word list.
            in_new_words = any(syllabus.values())
            continue

        if in_new_words:
            new_words.append(clean(line))
            continue

        for name, text in assign_columns(split_runs(line), columns):
            syllabus[name].append(clean(text))

    return {
        "number": int(LESSON_MARKER.search(lines[marker]).group(1)),
        "title": title_from_opener(lines, marker),
        "communication": syllabus["komunikacja"],
        "vocabularyTopics": syllabus["słownictwo"],
        "grammar": syllabus["gramatyka"],
        "newWords": split_new_words(new_words),
    }


def split_new_words(chunks: list[str]) -> list[str]:
    """Split the comma-separated new-word list, keeping bracketed forms together.

    Entries carry their own commas inside brackets — "spać (śpię, śpisz)" is one
    word with its conjugation, not three.
    """
    words = []
    for chunk in chunks:
        depth = 0
        item = ""
        for character in chunk + ",":
            if character in "([":
                depth += 1
            elif character in ")]":
                depth = max(0, depth - 1)
            if character == "," and depth == 0:
                item = clean(item)
                if item and not item.isdigit():
                    words.append(item)
                item = ""
            else:
                item += character
    return words


def audio_tags(body: str) -> list[str]:
    return sorted({f"{m.group(1)}{m.group(2)}{m.group(3)}{m.group(4)}" for m in AUDIO_TAG.finditer(body)})


def load_overrides() -> dict[tuple[str, int], dict[str, str]]:
    """Hand-corrected titles and word lists, keyed by (book, lesson number)."""
    overrides: dict[tuple[str, int], dict[str, str]] = {}
    for line in OVERRIDES.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        book, number, field, value = line.split("\t")
        overrides.setdefault((book, int(number)), {})[field] = value
    return overrides


def apply_override(
    lesson: dict,
    override: dict[str, str],
) -> None:
    if "title" in override:
        lesson["title"] = override["title"]
    if "words" in override:
        lesson["newWords"] = split_new_words([override["words"]])


def extract_book(book: str) -> list[dict]:
    parse = parse_ocr_opener if is_ocr_source(book) else parse_opener
    book_pages = pages(book)
    openers = [i for i, page in enumerate(book_pages) if parse(page)]
    if not openers:
        sys.exit(f"{book}: found no lesson openers; the text layer is probably unusable")

    lessons = []
    for position, start in enumerate(openers):
        end = openers[position + 1] if position + 1 < len(openers) else len(book_pages)
        lesson = parse(book_pages[start])
        body = "\n".join(book_pages[start:end])
        lesson["book"] = book
        lesson["firstPage"] = start + 1
        lesson["lastPage"] = end
        lesson["audioTags"] = audio_tags(body)
        lessons.append(lesson)

    # Openers come in book order, so position is a more reliable number than an OCR
    # digit. Trusting it is only safe when every opener was found, which the count
    # check below is there to establish.
    overrides = load_overrides()
    for position, lesson in enumerate(lessons, start=1):
        lesson["ocrNumber"] = lesson["number"]
        lesson["number"] = position
        apply_override(lesson, overrides.get((book, position), {}))

    expected = EXPECTED_LESSONS.get(book)
    if expected and len(lessons) != expected:
        sys.exit(
            f"{book}: expected {expected} lesson openers, found {len(lessons)} "
            f"(pages {[l['firstPage'] for l in lessons]})"
        )

    return lessons


def report(book: str, lessons: list[dict]) -> None:
    untitled = [lesson["number"] for lesson in lessons if not lesson["title"]]
    empty = [lesson["number"] for lesson in lessons if not lesson["newWords"]]
    print(f"{book}: {len(lessons)} lessons, {sum(len(l['newWords']) for l in lessons)} new words")
    if untitled:
        print(f"  no title: {untitled}")
    if empty:
        print(f"  no new words: {empty}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("books", nargs="*", default=["a1_coursebook"])
    parser.add_argument("--show", type=int, help="print one lesson in full")
    args = parser.parse_args()

    result = {}
    for book in args.books:
        lessons = extract_book(book)
        report(book, lessons)
        result[book] = lessons
        if args.show:
            match = next((l for l in lessons if l["number"] == args.show), None)
            print(json.dumps(match, ensure_ascii=False, indent=2))

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    path = CACHE_DIR / "lessons.json"
    path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"-> {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
