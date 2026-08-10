#!/usr/bin/env python3
"""Tests for the course build:  python3 tools/course/test_build_course.py

The build's job is to refuse to ship a broken course, so most of these assert that
a malformed catalogue raises rather than being written out. They run on synthetic
catalogues, not on the real extraction, so they pass on a machine with no books.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from build_course import BuildError, fold, resolve_word, validate  # noqa: E402

FAILURES = []


def check(name, condition):
    if not condition:
        FAILURES.append(name)


def expect_build_error(name, courses, fragment):
    try:
        validate(courses)
    except BuildError as error:
        check(f"{name}: message mentions {fragment!r}", fragment in str(error))
        return
    FAILURES.append(f"{name}: expected BuildError, got none")


def lesson(number, word_count=4, title="Lekcja", lesson_id=None):
    return {
        "id": lesson_id or f"krok-a1-{number:02d}",
        "courseId": "krok-a1",
        "number": number,
        "title": title,
        "vocabularyIds": list(range(1, word_count + 1)),
    }


def course(lessons):
    return [{"id": "krok-a1", "order": 1, "level": "A1", "title": {}, "lessons": lessons}]


def test_valid_course_passes():
    validate(course([lesson(1), lesson(2), lesson(3)]))


def test_lesson_with_no_words_is_rejected():
    expect_build_error("empty lesson", course([lesson(1), lesson(2, word_count=0)]), "expected at least")


def test_lesson_with_one_word_is_rejected():
    # One word cannot build a training step with distractors, so it is not a lesson.
    expect_build_error("one-word lesson", course([lesson(1, word_count=1)]), "expected at least")


def test_untitled_lesson_is_rejected():
    expect_build_error("untitled lesson", course([lesson(1, title="")]), "no title")


def test_gap_in_lesson_numbering_is_rejected():
    expect_build_error("numbering gap", course([lesson(1), lesson(3)]), "contiguous")


def test_duplicate_lesson_id_is_rejected():
    lessons = [lesson(1), lesson(2, lesson_id="krok-a1-01")]
    expect_build_error("duplicate id", course(lessons), "duplicate lesson id")


def test_course_with_no_lessons_is_rejected():
    expect_build_error("no lessons", course([]), "no lessons")


def test_fold_matches_the_apps_search_key():
    check("fold strips diacritics", fold("Dzień") == "dzien")
    check("fold lowercases", fold("PROSZĘ") == "prosze")


def test_word_resolution():
    index = {"kot": 1, "pies": 2, "spac": 3, "zaczynac sie": 4, "konczyc sie": 5}
    forms = {"spac (spie, spisz)": ["spać"], "zaczynac sie ≠ konczyc sie": ["zaczynać się", "kończyć się"]}

    check("a plain word resolves", resolve_word("kot", index, forms) == [1])
    check("an unknown word resolves to None", resolve_word("smok", index, forms) is None)
    check(
        "a printed form maps to its headword",
        resolve_word("spać (śpię, śpisz)", index, forms) == [3],
    )
    check(
        "a printed pair maps to both headwords",
        resolve_word("zaczynać się ≠ kończyć się", index, forms) == [4, 5],
    )


def main():
    for name, test in sorted(globals().items()):
        if name.startswith("test_") and callable(test):
            try:
                test()
            except Exception as error:  # noqa: BLE001 - a raising test is a failing test
                FAILURES.append(f"{name}: raised {error!r}")

    if FAILURES:
        print(f"{len(FAILURES)} failed:")
        for failure in FAILURES:
            print(f"  {failure}")
        return 1
    print("all course build tests passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
