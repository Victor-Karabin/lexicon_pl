#!/usr/bin/env python3
"""Maps the Krok po kroku recordings to their Google Drive file ids.

The 488 tracks are 454 MB, so they are not in the APK. They can be side-loaded
with install_audio.sh, but a phone that has not been side-loaded can fetch them
one at a time instead — which needs a file id per track.

Drive renders a plain HTML listing for a link-shared folder at
/embeddedfolderview, and it is not paginated for folders this size, so one
request per folder enumerates everything. The manifest is keyed by the same
filenames extract_audio.py produces, so build_course.py can attach an id to a
track without either script knowing about the other's naming.

    python3 tools/course/fetch_drive_manifest.py

Only the two coursebook folders are shared; the 88 workbook tracks are not
there, and stay side-load-only.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.request

from krok_paths import CACHE_DIR

FOLDER_VIEW = "https://drive.google.com/embeddedfolderview?id={id}#list"

# Folder ids from the shared "Audio" folder. A folder that is not link-shared
# returns a sign-in page instead of a listing, which shows up as zero entries.
DRIVE_FOLDERS = {
    "a1_coursebook": "1VeqaqLSkxydjFuqbMQDOBP5vLAgiO0fW",
    "a2_coursebook": "1Gp6LyhPB9_G7AlzyR6FyArew4smRVD4C",
}

EXPECTED_TRACKS = {"a1_coursebook": 220, "a2_coursebook": 180}

FILE_ID = re.compile(r"/file/d/([A-Za-z0-9_-]{20,})")
FILE_NAME = re.compile(r"flip-entry-title[\"']>([^<]+)<")

MANIFEST = CACHE_DIR / "drive_manifest.json"


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8", errors="replace")


def folder_entries(book: str, folder_id: str) -> dict[str, str]:
    html = fetch(FOLDER_VIEW.format(id=folder_id))
    ids = FILE_ID.findall(html)
    names = FILE_NAME.findall(html)

    if not names:
        sys.exit(f"{book}: no listing returned — is the folder still link-shared?")
    if len(ids) != len(names):
        sys.exit(f"{book}: {len(names)} names but {len(ids)} ids; the listing markup has changed")

    expected = EXPECTED_TRACKS[book]
    if len(names) != expected:
        sys.exit(f"{book}: expected {expected} tracks, the folder lists {len(names)}")

    # extract_audio.py prefixes the book, so the manifest matches its filenames.
    return {f"{book}_{name}": file_id for name, file_id in zip(names, ids)}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()

    manifest: dict[str, str] = {}
    for book, folder_id in DRIVE_FOLDERS.items():
        entries = folder_entries(book, folder_id)
        manifest.update(entries)
        print(f"{book}: {len(entries)} tracks")

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"{len(manifest)} tracks -> {MANIFEST}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
