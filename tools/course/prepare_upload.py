#!/usr/bin/env python3
"""Builds krok/for_upload: every course recording, named the way the app asks for it.

Run from anywhere:  python3 tools/course/prepare_upload.py

Reads
    krok/Audio/…   the unpacked recordings of every book

Writes
    krok/for_upload/<book>/<part>/<file>.mp3
    krok/for_upload/manifest.csv
    krok/for_upload/README.md

The app downloads a recording by its Google Drive id and stores it under the
file name the lesson asks for, so the names here are the contract: Krok po kroku
files keep exactly the names extract_audio.py gives them (including the four
misspelled source names the course already refers to). Each part is one flat
folder, because fetch_drive_manifest.py lists a Drive folder in one request;
the lesson number is in every file name, so nothing is lost by not nesting.

Recordings a lesson script refers to but that do not exist yet are listed in the
README as missing, under the exact name to upload them with.

Files are cloned (cp -c), so the folder costs no disk space on APFS and the
sources stay untouched. Running it again rebuilds the folder from scratch.
"""

from __future__ import annotations

import csv
import json
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

from extract_audio import track_id
from krok_paths import ASSET_DIR, KROK_ROOT

SOURCE = KROK_ROOT / "Audio"
TARGET = KROK_ROOT / "for_upload"


@dataclass(frozen=True)
class Part:
    book: str
    part: str
    source: str
    prefix: str
    used_by_app: bool
    expected: int


PARTS = [
    Part("krok_po_kroku_1_a1", "coursebook", "Podrecznik_1_A1-A2", "a1_coursebook", True, 220),
    Part("krok_po_kroku_1_a1", "workbook", "Zeszyt_cwiczen_1", "a1_workbook", True, 88),
    Part("krok_po_kroku_2_a2", "coursebook", "Podrecznik_2_A2-B1", "a2_coursebook", True, 180),
    Part("polski_na_dobry_start_a1", "coursebook", "Polski_na_dobry_start_A1/mp3", "pnds_coursebook", False, 126),
    Part("z_jezykiem_polskim_kazdego_dnia_1_a2", "coursebook", "Z_jezykiem_polskim_kazdego_dnia_1_A2/CD audio/Podręcznik", "zjpkd1_coursebook", False, 33),
    Part("z_jezykiem_polskim_kazdego_dnia_1_a2", "workbook", "Z_jezykiem_polskim_kazdego_dnia_1_A2/CD audio/Zeszyt ćwiczeń", "zjpkd1_workbook", False, 21),
    Part("z_jezykiem_polskim_kazdego_dnia_1_a2", "teacher_guide", "Z_jezykiem_polskim_kazdego_dnia_1_A2/CD audio/Poradnik", "zjpkd1_teacher_guide", False, 10),
    Part("z_jezykiem_polskim_kazdego_dnia_2_b1", "coursebook", "Z_jezykiem_polskim_kazdego_dnia_2_B1/Podrecznik", "zjpkd2_coursebook", False, 35),
    Part("z_jezykiem_polskim_kazdego_dnia_2_b1", "workbook", "Z_jezykiem_polskim_kazdego_dnia_2_B1/Cwiczenia", "zjpkd2_workbook", False, 21),
    Part("z_jezykiem_polskim_kazdego_dnia_2_b1", "teacher_guide", "Z_jezykiem_polskim_kazdego_dnia_2_B1/Poradnik_metodyczny", "zjpkd2_teacher_guide", False, 8),
]

KROK_BOOKS = {"a1_coursebook", "a1_workbook", "a2_coursebook"}


class BuildError(Exception):
    """A source folder does not hold what the app expects."""


def lesson_of(part: Part, stem: str) -> str:
    if part.prefix not in KROK_BOOKS:
        return ""
    parsed = track_id(part.prefix, stem)
    if parsed is None:
        raise BuildError(f"{part.source}/{stem}.mp3 does not match the {part.prefix} numbering")
    return str(parsed["lesson"])


def clone(source: Path, target: Path) -> None:
    if subprocess.run(["cp", "-c", str(source), str(target)], capture_output=True).returncode != 0:
        shutil.copy2(source, target)


def build_part(part: Part) -> list[dict]:
    folder = SOURCE / part.source
    files = sorted((p for p in folder.rglob("*.mp3") if p.is_file()), key=lambda p: p.name)
    if len(files) != part.expected:
        raise BuildError(f"{part.source}: expected {part.expected} recordings, found {len(files)}")

    target = TARGET / part.book / part.part
    target.mkdir(parents=True, exist_ok=True)
    rows = []
    for source in files:
        name = f"{part.prefix}_{source.stem}.mp3"
        clone(source, target / name)
        rows.append(
            {
                "file": name,
                "folder": f"{part.book}/{part.part}",
                "lesson": lesson_of(part, source.stem),
                "used_by_app": "yes" if part.used_by_app else "no",
                "bytes": source.stat().st_size,
                "source": str(source.relative_to(KROK_ROOT)),
            }
        )
    return rows


def missing_recordings(present: set[str]) -> list[tuple[str, str]]:
    missing = []
    for script in sorted(ASSET_DIR.glob("lesson_*.json")):
        data = json.loads(script.read_text(encoding="utf-8"))
        for track in data.get("tracks", []):
            if track["file"] not in present:
                missing.append((track["file"], data["lessonId"]))
    return missing


def folder_size(rows: list[dict]) -> str:
    return f"{sum(r['bytes'] for r in rows) / 1_000_000:.0f} MB"


def write_readme(rows: list[dict], missing: list[tuple[str, str]]) -> None:
    by_folder: dict[str, list[dict]] = {}
    for row in rows:
        by_folder.setdefault(row["folder"], []).append(row)

    lines = [
        "# Recordings for upload",
        "",
        "Upload the whole folder to Google Drive as it is and share it as \"Anyone with the link\".",
        "The app downloads each recording by its Drive id and stores it under the file name below,",
        "so **do not rename any file**.",
        "",
        "Built by `lexicon_pl/tools/course/prepare_upload.py` from `krok/Audio`; run it again to rebuild.",
        "`manifest.csv` lists every file with its lesson and source.",
        "",
        "| Folder | Files | Size | Used by the app |",
        "| --- | --- | --- | --- |",
    ]
    for folder, items in by_folder.items():
        lines.append(f"| `{folder}` | {len(items)} | {folder_size(items)} | {items[0]['used_by_app']} |")
    lines += [
        "",
        "## File names",
        "",
        "- Krok po kroku: `a1_coursebook_<book><lesson><section><task>.mp3` (`101a1` = book 1, lesson 01, section A, task 1),",
        "  `a1_workbook_<nn>_L<lesson>_cwiczenie<exercise>.mp3`, `a2_coursebook_<book><lesson><section><task>.mp3`.",
        "  Four A1 names keep a typo from the source discs (`0102e3`, `0122b1-1mp3`, `108a7mp3`, `123aa2-3`)",
        "  because the course already refers to them by those names.",
        "- Other books: `<book>_<part>_<original name>.mp3`. The app does not use them yet.",
        "",
        "## Missing recordings",
        "",
    ]
    if missing:
        lines += [
            "A lesson already refers to these, but there is no recording for them yet. When you have one,",
            "add it to the folder below under exactly this name:",
            "",
            "| File | Lesson | Folder |",
            "| --- | --- | --- |",
        ]
        for name, lesson in missing:
            book = "krok_po_kroku_1_a1/coursebook" if name.startswith("a1_coursebook") else "krok_po_kroku_1_a1/workbook"
            lines.append(f"| `{name}` | {lesson} | `{book}` |")
    else:
        lines.append("None: every recording a lesson refers to is here.")
    (TARGET / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    try:
        if TARGET.exists():
            shutil.rmtree(TARGET)
        rows = [row for part in PARTS for row in build_part(part)]
    except BuildError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    with (TARGET / "manifest.csv").open("w", newline="", encoding="utf-8") as out:
        writer = csv.DictWriter(out, fieldnames=["file", "folder", "lesson", "used_by_app", "bytes", "source"])
        writer.writeheader()
        writer.writerows(rows)

    missing = missing_recordings({row["file"] for row in rows})
    write_readme(rows, missing)

    print(f"{len(rows)} recordings, {folder_size(rows)} -> {TARGET}")
    if missing:
        print(f"{len(missing)} recordings a lesson refers to are missing; see README.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
