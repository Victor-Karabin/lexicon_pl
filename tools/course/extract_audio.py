#!/usr/bin/env python3
"""Unpack the Krok po kroku audio archives and map every track to a lesson.

The three .docx files are ZIP archives of MP3s, not documents. Their filenames
carry the book's own numbering, which is what lets a track be attached to the
exercise it belongs to:

    101a1.mp3          book 1, lesson 01, section A, task 1
    203a5-a.mp3        book 2, lesson 03, section A, task 5, part a
    01_L01_cwiczenie2  workbook track 01, lesson 01, exercise 2

Four A1 filenames are typos in the source archive and are corrected by name.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path

from krok_paths import AUDIO_ARCHIVES, AUDIO_DIR, CACHE_DIR, KROK_ROOT

COURSEBOOK_TRACK = re.compile(r"^(?P<book>[12])(?P<lesson>\d{2})(?P<section>[a-z])(?P<task>\d+)(?:-(?P<part>[0-9a-z]+))?$")
WORKBOOK_TRACK = re.compile(r"^(?P<index>\d+)_L(?P<lesson>\d{2})_cwiczenie(?P<task>\d+)(?:_(?P<part>\w+))?$")

# Malformed names in Audio_A1.docx. Each correction is the only numbering that
# leaves no gap in the surrounding series.
FILENAME_FIXES = {
    "0102e3": "102e3",
    "0122b1-1mp3": "122b1-1",
    "108a7mp3": "108a7",
    "123aa2-3": "123a2-3",
}

EXPECTED_TRACKS = {"a1_coursebook": 220, "a2_coursebook": 180, "a1_workbook": 88}


def track_id(book: str, stem: str) -> dict | None:
    stem = FILENAME_FIXES.get(stem, stem)

    if book == "a1_workbook":
        match = WORKBOOK_TRACK.match(stem)
        if not match:
            return None
        return {
            "lesson": int(match.group("lesson")),
            "section": None,
            "task": int(match.group("task")),
            "part": match.group("part"),
            "order": int(match.group("index")),
        }

    match = COURSEBOOK_TRACK.match(stem.lower())
    if not match:
        return None
    return {
        "lesson": int(match.group("lesson")),
        "section": match.group("section").upper(),
        "task": int(match.group("task")),
        "part": match.group("part"),
        "order": None,
    }


def extract(book: str, unpack: bool) -> list[dict]:
    archive = KROK_ROOT / AUDIO_ARCHIVES[book]
    if not archive.exists():
        sys.exit(f"missing audio archive: {archive}")

    target = AUDIO_DIR / book
    tracks: list[dict] = []
    unknown: list[str] = []

    with zipfile.ZipFile(archive) as zf:
        members = [n for n in zf.namelist() if n.lower().endswith(".mp3")]
        for name in sorted(members):
            stem = Path(name).stem
            parsed = track_id(book, stem)
            if parsed is None:
                unknown.append(stem)
                continue

            filename = f"{book}_{stem}.mp3"
            if unpack:
                target.mkdir(parents=True, exist_ok=True)
                (target / filename).write_bytes(zf.read(name))

            tracks.append(
                {
                    "book": book,
                    "lesson": parsed["lesson"],
                    "section": parsed["section"],
                    "task": parsed["task"],
                    "part": parsed["part"],
                    "order": parsed["order"],
                    "file": filename,
                    "bytes": zf.getinfo(name).file_size,
                }
            )

    if unknown:
        sys.exit(f"{book}: {len(unknown)} filenames do not match the numbering: {', '.join(unknown[:10])}")

    expected = EXPECTED_TRACKS[book]
    if len(tracks) != expected:
        sys.exit(f"{book}: expected {expected} tracks, found {len(tracks)}")

    tracks.sort(key=lambda t: (t["lesson"], t["section"] or "", t["task"], t["part"] or "", t["order"] or 0))
    return tracks


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--unpack", action="store_true", help="also write the MP3 files to the cache")
    args = parser.parse_args()

    manifest = {}
    for book in AUDIO_ARCHIVES:
        tracks = extract(book, args.unpack)
        manifest[book] = tracks
        lessons = sorted({t["lesson"] for t in tracks})
        print(f"{book}: {len(tracks)} tracks across lessons {lessons[0]}-{lessons[-1]} ({len(lessons)} lessons)")

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    path = CACHE_DIR / "audio_manifest.json"
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"total {sum(len(v) for v in manifest.values())} tracks -> {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
