#!/usr/bin/env python3
"""Tests for the course build:  python3 tools/course/test_build_course.py

The build's job is to refuse to ship a broken course, so most of these assert that
a malformed catalogue raises rather than being written out. They run on synthetic
catalogues, not on the real extraction, so they pass on a machine with no books.
"""

import json
import re
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from build_course import COURSE_ASSET, BuildError, WordIndex, build_lesson, fold, resolve_word, validate  # noqa: E402

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
    texts = ["kot", "pies", "spać", "zaczynać się", "kończyć się", "część", "cześć", "piec", "pięć"]
    index = WordIndex.of([{"id": number, "text": text} for number, text in enumerate(texts, start=1)])
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
    check("an exact spelling beats a word it only folds to", resolve_word("cześć", index, forms) == [7])
    check("an exact spelling beats a folded one", resolve_word("pięć", index, forms) == [9])
    check("a word OCR read without diacritics still resolves", resolve_word("spac", index, forms) == [3])


def exercise(exercise_id, items, audio_file="a1_coursebook_101a1.mp3"):
    return {
        "id": exercise_id,
        "type": "gap_fill",
        "instruction": "Proszę posłuchać i uzupełnić.",
        "audioFile": audio_file,
        "items": items,
    }


def course_with(exercises):
    lessons = [lesson(1), lesson(2)]
    lessons[1] = {**lessons[1], "exercises": exercises}
    return course(lessons)


def gap(prompt, *answers):
    return {"prompt": prompt, "answers": list(answers)}


def test_duplicate_exercise_id_is_rejected():
    exercises = [exercise("krok-a1-02-102A1", [gap("To ___ kot.", "jest")]), exercise("krok-a1-02-102A1", [gap("To ___ pies.", "jest")])]
    expect_build_error("duplicate exercise id", course_with(exercises), "duplicate exercise id")


def test_exercise_ids_that_differ_are_accepted():
    exercises = [exercise("krok-a1-02-102A1", [gap("To ___ kot.", "jest")]), exercise("krok-a1-02-102A2", [gap("To ___ pies.", "jest")])]
    validate(course_with(exercises))


def test_answer_that_is_only_punctuation_is_rejected():
    items = [gap("Czy coś przekazać ___", "?")]
    expect_build_error("punctuation answer", course_with([exercise("krok-a1-02-102B1", items)]), "punctuation")


def test_punctuation_error_names_the_exercise():
    items = [gap("Czy coś przekazać ___", "?")]
    expect_build_error("names the exercise", course_with([exercise("krok-a1-02-102B1", items)]), "krok-a1-02-102B1")


def test_answer_with_punctuation_around_a_word_is_accepted():
    validate(course_with([exercise("krok-a1-02-102C2", [gap("A kosmetyki w ___", "drogerii.")])]))


def test_context_lines_without_answers_are_accepted():
    validate(course_with([exercise("krok-a1-02-102A3", [gap("Dzień dobry. Jestem Agnieszka.")])]))


def test_gap_glued_to_the_front_of_a_word_is_rejected():
    items = [gap("Dziękuję, to cenna informa___ ", "cja.")]
    expect_build_error("gap after letters", course_with([exercise("krok-a1-02-102D1", items)]), "splits a word")


def test_gap_glued_to_the_end_of_a_word_is_rejected():
    items = [gap("Dziękuję, to cenna ___cja.", "informa")]
    expect_build_error("gap before letters", course_with([exercise("krok-a1-02-102D2", items)]), "splits a word")


def test_gaps_between_words_and_missing_letters_are_accepted():
    validate(course_with([exercise("krok-a1-02-102E1", [gap("To ___ samoobsługowy. (___)", "sklep", "tak"), gap("Dz_eń dobry.", "ź")])]))


def test_same_tag_exercises_get_their_own_id_and_audio():
    tracks = [
        {"book": "a1_coursebook", "lesson": 40, "section": "B", "task": 4, "part": part, "file": file}
        for part, file in ((None, "a1_coursebook_140b4.mp3"), ("2", "a1_coursebook_140b4-2.mp3"), ("3", "a1_coursebook_140b4-3.mp3"))
    ]
    exercises = {
        40: [
            {"tag": "140B4", "type": "gap_fill", "instruction": "x", "items": [gap("___", answer)]}
            for answer in ("Mami, co kupujemy?", "Poproszę czosnek.", "Poproszę marchewkę.")
        ]
    }

    built = build_lesson(
        {"id": "krok-a1"}, {"number": 40, "title": "T", "newWords": []}, WordIndex.of([]), {}, tracks, [], [], {}, exercises, {}, []
    )["exercises"]

    check("every exercise has its own id", len({e["id"] for e in built}) == 3)
    check("every exercise plays its own part", [e["audioFile"] for e in built] == [t["file"] for t in tracks])
    check("ids still start with the tag", all(e["id"].startswith("krok-a1-40-140B4") for e in built))


def test_a_tag_used_once_keeps_its_id_and_audio():
    tracks = [{"book": "a1_coursebook", "lesson": 40, "section": "A", "task": 1, "part": None, "file": "a1_coursebook_140a1.mp3"}]
    exercises = {40: [{"tag": "140A1", "type": "gap_fill", "instruction": "x", "items": [gap("___", "tak")]}]}

    built = build_lesson(
        {"id": "krok-a1"}, {"number": 40, "title": "T", "newWords": []}, WordIndex.of([]), {}, tracks, [], [], {}, exercises, {}, []
    )["exercises"]

    check("id unchanged", [e["id"] for e in built] == ["krok-a1-40-140A1"])
    check("audio unchanged", [e["audioFile"] for e in built] == ["a1_coursebook_140a1.mp3"])


def homographs():
    words = [
        {"id": 936, "text": "sztuka", "translation": "piece"},
        {"id": 2630, "text": "sztuka", "translation": "art"},
        {"id": 7, "text": "kot", "translation": "cat"},
    ]
    return WordIndex.of(words)


def expect_resolve_error(name, call, fragments):
    try:
        call()
    except BuildError as error:
        for fragment in fragments:
            check(f"{name}: message mentions {fragment!r}", fragment in str(error))
        return
    FAILURES.append(f"{name}: expected BuildError, got none")


def test_homograph_is_chosen_by_the_english_gloss():
    index = homographs()
    check("art", resolve_word("sztuka", index, {}, gloss="art") == [2630])
    check("piece", resolve_word("sztuka", index, {}, gloss="piece") == [936])
    check("gloss is not case sensitive", resolve_word("sztuka", index, {}, gloss="Art") == [2630])


def test_homograph_without_a_gloss_fails_the_build():
    expect_resolve_error("no gloss", lambda: resolve_word("sztuka", homographs(), {}), ["sztuka", "936", "2630"])


def test_homograph_with_a_gloss_that_matches_nothing_fails_the_build():
    expect_resolve_error("wrong gloss", lambda: resolve_word("sztuka", homographs(), {}, gloss="painting"), ["sztuka"])


def test_a_word_with_one_meaning_ignores_the_gloss():
    index = homographs()
    check("no gloss", resolve_word("kot", index, {}) == [7])
    check("any gloss", resolve_word("kot", index, {}, gloss="feline") == [7])


def shipped_lessons():
    courses = json.loads(COURSE_ASSET.read_text(encoding="utf-8"))["courses"]
    return {lesson["id"]: lesson for course in courses for lesson in course["lessons"]}


def test_shipped_lesson_five_links_art_not_piece():
    lesson_five = shipped_lessons()["krok-a1-05"]
    check("art (2630) is linked", 2630 in lesson_five["vocabularyIds"])
    check("piece (936) is not linked", 936 not in lesson_five["vocabularyIds"])


def test_shipped_exercise_ids_are_unique():
    ids = [e["id"] for lesson in shipped_lessons().values() for e in lesson["exercises"]]
    check(f"duplicates: {[i for i, n in Counter(ids).items() if n > 1]}", len(ids) == len(set(ids)))


def test_shipped_answers_are_never_only_punctuation():
    offenders = [
        e["id"]
        for lesson in shipped_lessons().values()
        for e in lesson["exercises"]
        for item in e["items"]
        for answer in item.get("answers", [])
        if not re.search(r"\w", answer)
    ]
    check(f"punctuation-only answers in {offenders}", offenders == [])


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
