#!/usr/bin/env python3
"""Builds the shipped program catalogue from the authored per-level JSON.

Run from anywhere:  python3 tools/program/build_programs.py

Reads
    tools/program/programs/*.json          one file per level: a1, a2, b1 …
    data/src/androidMain/assets/vocabulary_presets.json
    data/src/androidMain/assets/course_krok.json

Writes
    data/src/androidMain/assets/programs.json

A program is configuration the engine follows without question, so a program that
names a preset which does not exist, or whose progress weights do not add up, would
fail silently on a phone as a course that quietly does nothing. Everything checkable
is checked here instead, and nothing is written unless all of it passes.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PROGRAM_DIR = Path(__file__).parent / "programs"
ASSETS = REPO_ROOT / "data" / "src" / "androidMain" / "assets"
OUTPUT = ASSETS / "programs.json"

# Kept in step with the enums in interactors/program/ProgramConfig.kt. A name that
# drifts out of that set parses to nothing useful, so it is worth failing on here.
TARGET_TYPES = {"VOCABULARY", "LESSONS", "TIME", "EXERCISES", "STREAK", "RETENTION"}
SCOPE_SOURCES = {"PRESET", "FAVOURITES", "CEFR_LEVEL", "LESSON", "ALL"}
ORDERINGS = {"AS_LISTED", "FREQUENCY", "DIFFICULTY", "ALPHABETICAL", "RANDOM"}
STRATEGIES = {"NEW_FIRST", "REVIEWS_FIRST", "MIXED", "TOPIC_BY_TOPIC", "ADAPTIVE"}
ACTIVITY_TYPES = {"LEARN", "REVIEW", "PRONOUNCE", "LISTEN", "WRITE", "MIXED", "CHALLENGE"}
DIFFICULTIES = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}
VISIBILITIES = {"PUBLIC", "PRIVATE"}
ADAPTATION_TRIGGERS = {
    "HIGH_ACCURACY", "LOW_ACCURACY", "MISSED_DAY", "MISSED_WEEK",
    "FAST_PROGRESS", "SLOW_PROGRESS", "REVIEW_BACKLOG",
}
ADAPTATION_ACTIONS = {
    "INCREASE_NEW_WORDS", "DECREASE_NEW_WORDS", "INCREASE_REVIEWS",
    "PAUSE_NEW_WORDS", "EXTEND_DURATION", "REPEAT_MILESTONE",
}
REWARD_TRIGGERS = {
    "MILESTONE", "DAILY_GOAL", "WEEKLY_GOAL", "PROGRAM_COMPLETED",
    "STREAK", "ACCURACY", "STUDY_TIME",
}
REWARD_TYPES = {"BADGE", "XP", "CERTIFICATE", "TROPHY"}
CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"}

# TrainingIds in presentation/main/TrainingCatalog.kt. An activity naming a training
# that does not exist has nothing to launch.
TRAININGS = {
    "dictation", "dictation_puzzle", "true_or_false", "word_match",
    "pronunciation_check", "puzzle", "image_test", "memory_cards", "mix", "crossword",
}

PROGRESS_WEIGHT_TOTAL = 100


class BuildError(Exception):
    """A problem the author has to fix; never raised at runtime in the app."""


def load_json(path: Path):
    if not path.exists():
        raise BuildError(f"missing input: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def known_preset_ids() -> set[str]:
    return {p["id"] for p in load_json(ASSETS / "vocabulary_presets.json")["presets"]}


def known_lesson_ids() -> set[str]:
    path = ASSETS / "course_krok.json"
    if not path.exists():
        return set()
    return {
        lesson["id"]
        for course in load_json(path)["courses"]
        for lesson in course["lessons"]
    }


def check_in(value, allowed: set[str], what: str, where: str) -> None:
    if value not in allowed:
        raise BuildError(f"{where}: unknown {what} {value!r}; expected one of {sorted(allowed)}")


def validate_scope(scope: dict, where: str, presets: set[str], lessons: set[str]) -> None:
    check_in(scope.get("ordering", "FREQUENCY"), ORDERINGS, "ordering", where)
    for key in ("include", "exclude"):
        for source in scope.get(key, []):
            kind = source["type"]
            check_in(kind, SCOPE_SOURCES, "scope source", f"{where}.{key}")
            value = source.get("value", "")
            if kind == "PRESET" and value not in presets:
                raise BuildError(f"{where}.{key}: no preset {value!r} in vocabulary_presets.json")
            if kind == "LESSON" and lessons and value not in lessons:
                raise BuildError(f"{where}.{key}: no lesson {value!r} in course_krok.json")
            if kind == "CEFR_LEVEL":
                check_in(value, CEFR_LEVELS, "CEFR level", f"{where}.{key}")

    if not scope.get("include"):
        raise BuildError(f"{where}: nothing included, so the program has no words to teach")


def validate_activities(activities: list, where: str) -> None:
    seen: set[str] = set()
    for activity in activities:
        ident = activity["id"]
        if ident in seen:
            raise BuildError(f"{where}: duplicate activity id {ident!r}")
        seen.add(ident)
        check_in(activity["type"], ACTIVITY_TYPES, "activity type", f"{where}.{ident}")
        for training in activity.get("trainings", []):
            if training not in TRAININGS:
                raise BuildError(f"{where}.{ident}: no training {training!r}; expected one of {sorted(TRAININGS)}")
        if not activity.get("trainings"):
            raise BuildError(f"{where}.{ident}: no trainings, so nothing can satisfy it")
        if activity.get("target", 1) <= 0:
            raise BuildError(f"{where}.{ident}: target must be positive")


def validate_program(program: dict, presets: set[str], lessons: set[str]) -> None:
    ident = program["id"]
    check_in(program["level"], CEFR_LEVELS, "level", ident)
    check_in(program.get("difficulty", "BEGINNER"), DIFFICULTIES, "difficulty", ident)
    check_in(program.get("visibility", "PUBLIC"), VISIBILITIES, "visibility", ident)
    if not program.get("title"):
        raise BuildError(f"{ident}: no title")

    config = program.get("config", {})
    check_in(config.get("strategy", "MIXED"), STRATEGIES, "strategy", ident)

    for goal in config.get("goals", []):
        check_in(goal["type"], TARGET_TYPES, "goal type", f"{ident}.goals.{goal['id']}")
        if goal["target"] <= 0:
            raise BuildError(f"{ident}.goals.{goal['id']}: target must be positive")

    validate_scope(config.get("scope", {}), f"{ident}.scope", presets, lessons)

    daily = config.get("dailyPlan", {})
    validate_activities(daily.get("activities", []), f"{ident}.dailyPlan")
    if weekend := daily.get("weekend"):
        validate_activities(weekend.get("activities", []), f"{ident}.dailyPlan.weekend")
    if not daily.get("activities"):
        raise BuildError(f"{ident}.dailyPlan: no activities, so a day would be empty")

    # Milestones read as a ladder in the UI, so a rung that goes backwards is a
    # mistake worth catching rather than rendering.
    previous: dict[str, int] = {}
    for milestone in config.get("milestones", []):
        where = f"{ident}.milestones.{milestone['id']}"
        if not milestone.get("conditions"):
            raise BuildError(f"{where}: no conditions, so it can never be reached")
        for condition in milestone["conditions"]:
            kind = condition["type"]
            check_in(kind, TARGET_TYPES, "condition type", where)
            if kind in previous and condition["target"] < previous[kind]:
                raise BuildError(
                    f"{where}: {kind} target {condition['target']} is below the previous milestone's "
                    f"{previous[kind]}; milestones must climb"
                )
            previous[kind] = condition["target"]

    weights = config.get("progress", {})
    total = sum(weights.values())
    if weights and total != PROGRESS_WEIGHT_TOTAL:
        raise BuildError(f"{ident}.progress: weights sum to {total}, expected {PROGRESS_WEIGHT_TOTAL}")

    for rule in config.get("adaptation", []):
        where = f"{ident}.adaptation.{rule['id']}"
        check_in(rule["trigger"], ADAPTATION_TRIGGERS, "trigger", where)
        check_in(rule["action"], ADAPTATION_ACTIONS, "action", where)

    for reward in config.get("rewards", []):
        where = f"{ident}.rewards.{reward['id']}"
        check_in(reward["trigger"], REWARD_TRIGGERS, "reward trigger", where)
        check_in(reward.get("type", "BADGE"), REWARD_TYPES, "reward type", where)

    for condition in config.get("completion", {}).get("conditions", []):
        check_in(condition["type"], TARGET_TYPES, "completion condition", f"{ident}.completion")

    # A vocabulary goal beyond what the scope can supply can never be finished.
    reachable = reachable_word_count(config.get("scope", {}), presets)
    for goal in config.get("goals", []):
        if goal["type"] == "VOCABULARY" and reachable is not None and goal["target"] > reachable:
            raise BuildError(
                f"{ident}.goals.{goal['id']}: wants {goal['target']} words but the scope only reaches {reachable}"
            )


def reachable_word_count(scope: dict, presets: set[str]) -> int | None:
    """How many words the scope can supply, or None when it cannot be counted here."""
    catalogue = {p["id"]: p for p in load_json(ASSETS / "vocabulary_presets.json")["presets"]}
    words: set[int] = set()
    for source in scope.get("include", []):
        if source["type"] != "PRESET":
            return None
        words.update(catalogue[source["value"]].get("vocabularyIds", []))
    for source in scope.get("exclude", []):
        if source["type"] == "PRESET":
            words.difference_update(catalogue[source["value"]].get("vocabularyIds", []))
    return len(words)


def build() -> int:
    presets = known_preset_ids()
    lessons = known_lesson_ids()

    sources = sorted(PROGRAM_DIR.glob("*.json"))
    if not sources:
        raise BuildError(f"no programs to build in {PROGRAM_DIR}")

    programs = []
    seen: set[str] = set()
    for path in sources:
        program = load_json(path)
        if program["id"] in seen:
            raise BuildError(f"duplicate program id {program['id']!r}")
        seen.add(program["id"])
        validate_program(program, presets, lessons)
        programs.append(program)

    programs.sort(key=lambda p: (p.get("order", 0), p["level"]))

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        json.dumps({"programs": programs}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    for program in programs:
        activities = len(program["config"]["dailyPlan"].get("activities", []))
        milestones = len(program["config"].get("milestones", []))
        print(f"  {program['id']:<18} {program['level']}  {activities} activities, {milestones} milestones")
    print(f"{len(programs)} programs -> {OUTPUT.relative_to(REPO_ROOT)}")
    return 0


def main() -> int:
    try:
        return build()
    except BuildError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
