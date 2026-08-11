#!/usr/bin/env python3
"""Builds the shipped course asset from the extracted Krok po kroku structure.

Run from anywhere:  python3 tools/course/build_course.py

Reads
    krok/.cache/lessons.json          extract_krok.py output
    krok/.cache/audio_manifest.json   extract_audio.py output
    data/src/main/assets/vocabulary_pl.json

Writes
    data/src/main/assets/course_krok.json

Lesson vocabulary is stored as ids into the existing corpus, never as its own
copy of the words: a lesson gets its transcription, CEFR band and translation
from the same rows every training already uses. Krok po kroku is monolingual, so
a word the corpus does not already carry has to be authored into
tools/vocabulary/corpus/topics/zz-krok.tsv first — `--report-missing` lists them.
Forms the books print with a conjugation or an aspect partner are mapped back to
their headword by word_forms.tsv.

Validation runs before anything is written, so a lesson that would ship empty
fails here rather than on a phone.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from krok_paths import CACHE_DIR, COURSE_ASSET, REPO_ROOT

VOCABULARY_ASSET = REPO_ROOT / "data" / "src" / "main" / "assets" / "vocabulary_pl.json"
WORD_FORMS = Path(__file__).parent / "word_forms.tsv"

POLISH_DIACRITICS = {"ą": "a", "ć": "c", "ę": "e", "ł": "l", "ń": "n", "ó": "o", "ś": "s", "ź": "z", "ż": "z"}

# Mirrors the books: A1 covers the first course, A2 the second.
COURSES = [
    {
        "id": "krok-a1",
        "book": "a1_coursebook",
        "workbook": "a1_workbook",
        "order": 1,
        "level": "A1",
        "title": {"en": "Polski krok po kroku 1", "pl": "Polski krok po kroku 1"},
    },
    {
        "id": "krok-a2",
        "book": "a2_coursebook",
        "workbook": None,
        "order": 2,
        "level": "A2",
        "title": {"en": "Polski krok po kroku 2", "pl": "Polski krok po kroku 2"},
        # Not shipped yet. extract_krok.py recovers all 23 A2 lessons and their
        # titles, but the new-word lists are printed in coloured bold on a coloured
        # band, which the scan reads badly: headwords come back misspelled
        # ("niepefnosprawny", "zatoba", "repreSje") and only 35 of 222 match the
        # corpus. Shipping those would teach misspelled Polish, so A2 waits until
        # its word lists are transcribed into lesson_overrides.tsv the way lessons
        # 6, 12, 19 and 21 already are.
        "include": False,
    },
]

# A lesson below this has not been parsed properly, whatever the parser thinks.
MIN_WORDS_PER_LESSON = 2


class BuildError(Exception):
    """A problem the author has to fix; never raised at runtime in the app."""


def fold(text: str) -> str:
    """Matches common/SearchText.kt foldForSearch so both sides agree on a word's key."""
    return "".join(POLISH_DIACRITICS.get(c, c) for c in text.lower()).strip()


def load_json(path: Path) -> dict | list:
    if not path.exists():
        raise BuildError(f"missing input: {path}\n  run the extract step first")
    return json.loads(path.read_text(encoding="utf-8"))


def vocabulary_index() -> dict[str, int]:
    index: dict[str, int] = {}
    for word in load_json(VOCABULARY_ASSET):
        index.setdefault(fold(word["text"]), word["id"])
    return index


def word_forms() -> dict[str, list[str]]:
    """Printed form -> corpus headwords, for the entries the books do not print plainly."""
    forms: dict[str, list[str]] = {}
    for line in WORD_FORMS.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        printed, _, headwords = line.partition("\t")
        forms[fold(printed)] = [h.strip() for h in headwords.split(",") if h.strip()]
    return forms


def resolve_word(word: str, index: dict[str, int], forms: dict[str, list[str]]) -> list[int] | None:
    """Vocabulary ids for a printed lesson word, or None when nothing matches."""
    key = fold(word)
    if key in forms:
        return [index[fold(h)] for h in forms[key] if fold(h) in index]
    word_id = index.get(key)
    return [word_id] if word_id is not None else None


def load_exercises() -> dict[int, list[dict]]:
    """Exercises from extract_exercises.py, grouped by lesson number."""
    path = CACHE_DIR / "exercises.json"
    if not path.exists():
        print("no exercises.json; lessons will ship without exercises")
        return {}
    grouped: dict[int, list[dict]] = {}
    for exercise in json.loads(path.read_text(encoding="utf-8")):
        grouped.setdefault(exercise["lesson"], []).append(exercise)
    return grouped


def load_remote_manifest() -> dict[str, str]:
    """Drive ids from fetch_drive_manifest.py, or nothing if it has not been run."""
    path = CACHE_DIR / "drive_manifest.json"
    if not path.exists():
        print("no drive_manifest.json; audio will be side-load-only")
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def audio_tag_of(track: dict) -> str:
    """The 101A3-style label a track carries, rebuilt from how it was filed."""
    return f"1{track['lesson']:02d}{track['section'] or ''}{track['task']}"


def lesson_audio(
    tracks: list[dict],
    lesson_number: int,
    remote: dict[str, str],
) -> list[dict]:
    """Tracks for one lesson, each carrying its Drive id when the folder has it.

    A track with no id is side-load-only: the workbook recordings are not shared.
    """
    return [
        {
            "file": track["file"],
            "section": track["section"],
            "task": track["task"],
            "part": track["part"],
            "remoteId": remote.get(track["file"]),
        }
        for track in tracks
        if track["lesson"] == lesson_number
    ]


def build_lesson(
    course: dict,
    lesson: dict,
    index: dict[str, int],
    forms: dict[str, list[str]],
    coursebook_tracks: list[dict],
    remote: dict[str, str],
    exercises: dict[int, list[dict]],
    missing: list[tuple[str, int, str]],
) -> dict:
    vocabulary_ids: list[int] = []
    for word in lesson["newWords"]:
        resolved = resolve_word(word, index, forms)
        if resolved is None:
            missing.append((course["id"], lesson["number"], word))
            continue
        for word_id in resolved:
            if word_id not in vocabulary_ids:
                vocabulary_ids.append(word_id)

    return {
        "id": f"{course['id']}-{lesson['number']:02d}",
        "courseId": course["id"],
        "number": lesson["number"],
        "title": lesson["title"],
        "vocabularyIds": vocabulary_ids,
        "audio": lesson_audio(coursebook_tracks, lesson["number"], remote),
        "exercises": [
            {
                "id": f"{course['id']}-{lesson['number']:02d}-{e['tag']}",
                "type": e["type"],
                "instruction": e["instruction"],
                "audioFile": next(
                    (t["file"] for t in coursebook_tracks if audio_tag_of(t) == e["tag"]), None
                ),
                "items": e["items"],
            }
            for e in exercises.get(lesson["number"], [])
        ],
    }


def validate(courses: list[dict]) -> None:
    seen_lesson_ids: set[str] = set()
    for course in courses:
        if not course["lessons"]:
            raise BuildError(f"{course['id']}: no lessons")
        numbers = [lesson["number"] for lesson in course["lessons"]]
        if numbers != list(range(1, len(numbers) + 1)):
            raise BuildError(f"{course['id']}: lessons are not a contiguous 1..N run: {numbers}")

        for lesson in course["lessons"]:
            if lesson["id"] in seen_lesson_ids:
                raise BuildError(f"duplicate lesson id: {lesson['id']}")
            seen_lesson_ids.add(lesson["id"])

            if not lesson["title"]:
                raise BuildError(f"{lesson['id']}: no title")
            if len(lesson["vocabularyIds"]) < MIN_WORDS_PER_LESSON:
                raise BuildError(
                    f"{lesson['id']}: only {len(lesson['vocabularyIds'])} words resolved, "
                    f"expected at least {MIN_WORDS_PER_LESSON}"
                )


def build(report_missing: bool) -> int:
    lessons_by_book = load_json(CACHE_DIR / "lessons.json")
    audio_by_book = load_json(CACHE_DIR / "audio_manifest.json")
    remote = load_remote_manifest()
    exercises = load_exercises()
    index = vocabulary_index()
    forms = word_forms()

    missing: list[tuple[str, int, str]] = []
    courses = []
    for course in COURSES:
        if not course.get("include", True):
            print(f"{course['id']}: not shipped yet (see COURSES in this file)")
            continue

        lessons = lessons_by_book.get(course["book"])
        if not lessons:
            print(f"{course['id']}: no extracted lessons, skipping")
            continue

        coursebook_tracks = audio_by_book.get(course["book"], [])
        courses.append(
            {
                "id": course["id"],
                "order": course["order"],
                "level": course["level"],
                "title": course["title"],
                "lessons": [
                    build_lesson(
                        course, lesson, index, forms, coursebook_tracks, remote, exercises, missing
                    )
                    for lesson in lessons
                ],
            }
        )

    if report_missing:
        print(f"{len(missing)} lesson words are not in the corpus:")
        for course_id, number, word in missing:
            print(f"{word}\t\t\t\t# {course_id} lesson {number}")
        return 0

    validate(courses)

    COURSE_ASSET.parent.mkdir(parents=True, exist_ok=True)
    COURSE_ASSET.write_text(
        json.dumps({"courses": courses}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    total_lessons = sum(len(c["lessons"]) for c in courses)
    total_words = sum(len(l["vocabularyIds"]) for c in courses for l in c["lessons"])
    total_audio = sum(len(l["audio"]) for c in courses for l in c["lessons"])
    fetchable = sum(
        1 for c in courses for l in c["lessons"] for t in l["audio"] if t["remoteId"]
    )
    # A listening exercise whose tag names no recording cannot be run, so it is
    # dropped here rather than shipped as a dead entry.
    dropped = 0
    for course in courses:
        for lesson in course["lessons"]:
            playable = [e for e in lesson["exercises"] if e["audioFile"]]
            dropped += len(lesson["exercises"]) - len(playable)
            lesson["exercises"] = playable

    total_exercises = sum(len(l["exercises"]) for c in courses for l in c["lessons"])
    print(
        f"{len(courses)} courses, {total_lessons} lessons, {total_words} word links, "
        f"{total_audio} tracks ({fetchable} fetchable), {total_exercises} exercises"
    )
    if dropped:
        print(f"  {dropped} exercises dropped: their audio tag names no recording")
    if missing:
        print(f"warning: {len(missing)} lesson words are not in the corpus (--report-missing to list them)")
    print(f"-> {COURSE_ASSET.relative_to(REPO_ROOT)}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--report-missing",
        action="store_true",
        help="list lesson words absent from the corpus, as corpus TSV stubs",
    )
    args = parser.parse_args()

    try:
        return build(args.report_missing)
    except BuildError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
