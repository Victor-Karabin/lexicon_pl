#!/usr/bin/env python3
"""Maps the Krok po kroku recordings to their Google Drive file ids.

The 488 tracks are 454 MB, so they are not in the APK. They can be side-loaded
with install_audio.sh, but a phone that has not been side-loaded can fetch them
one at a time instead — which needs a file id per track.

The recordings live in the shared "for_upload" folder that prepare_upload.py
builds, one subfolder per book part, every file already named the way the app
asks for it. Drive renders a plain HTML listing for a link-shared folder at
/embeddedfolderview, and it is not paginated for folders this size, so one
request per folder enumerates everything: one for the root, one per book, one
per part.

    python3 tools/course/fetch_drive_manifest.py

A recording added to a part folder later (one a lesson already refers to) is
picked up by running this again, then build_course.py.
"""

from __future__ import annotations

import argparse
import html
import json
import re
import sys
import urllib.request

from krok_paths import CACHE_DIR

FOLDER_VIEW = "https://drive.google.com/embeddedfolderview?id={id}#list"

# https://drive.google.com/drive/folders/1YIuR3f1pnFE2CcGkd52-0J8l_LbDhTnB
ROOT_FOLDER = "1YIuR3f1pnFE2CcGkd52-0J8l_LbDhTnB"

# Part folders the app downloads from, and how many recordings each must hold at least.
PARTS = {
    ("krok_po_kroku_1_a1", "coursebook"): 220,
    ("krok_po_kroku_1_a1", "workbook"): 88,
    ("krok_po_kroku_2_a2", "coursebook"): 180,
}

ENTRY = re.compile(
    r'<a href="https://drive\.google\.com/(file/d|drive/folders)/([A-Za-z0-9_-]{20,})[^"]*"[^>]*>.*?flip-entry-title["\']>([^<]+)<',
    re.S,
)

MANIFEST = CACHE_DIR / "drive_manifest.json"


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8", errors="replace")


def listing(folder_id: str, where: str) -> tuple[dict[str, str], dict[str, str]]:
    """Subfolders and files of a link-shared folder, each by name."""
    page = fetch(FOLDER_VIEW.format(id=folder_id))
    if "flip-entry" not in page:
        sys.exit(f"{where}: no listing returned — is the folder still link-shared?")
    folders: dict[str, str] = {}
    files: dict[str, str] = {}
    for kind, entry_id, name in ENTRY.findall(page):
        (folders if kind == "drive/folders" else files)[html.unescape(name)] = entry_id
    return folders, files


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()

    books, _ = listing(ROOT_FOLDER, "for_upload")
    manifest: dict[str, str] = {}
    for (book, part), expected in PARTS.items():
        if book not in books:
            sys.exit(f"for_upload: no {book} folder")
        parts, _ = listing(books[book], book)
        if part not in parts:
            sys.exit(f"{book}: no {part} folder")
        _, files = listing(parts[part], f"{book}/{part}")
        recordings = {name: file_id for name, file_id in files.items() if name.endswith(".mp3")}
        if len(recordings) < expected:
            sys.exit(f"{book}/{part}: expected at least {expected} recordings, the folder lists {len(recordings)}")
        manifest.update(recordings)
        print(f"{book}/{part}: {len(recordings)} recordings")

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"{len(manifest)} recordings -> {MANIFEST}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
