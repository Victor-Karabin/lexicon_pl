#!/usr/bin/env python3
"""Render scanned Krok po kroku PDFs to text with tesseract.

The A1 coursebook is a native digital PDF and needs no OCR. The A2 coursebook
carries an embedded OCR layer produced by I.R.I.S. that substitutes Cyrillic
homoglyphs for roughly one letter in ten, and the A1 workbook has no text layer
at all. Both are re-rendered here and read with tesseract's Polish model.

Output goes to KROK/.cache/ocr/<book>.txt with a form feed between pages, which
is the same shape pdftotext produces, so downstream parsers do not care which
path a book took.
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from krok_paths import KROK_ROOT, OCR_DIR, book_path

RENDER_DPI = 400

# psm 4 keeps a page's columns in reading order, which is what makes the lesson
# opener's three-column syllabus recoverable. It assumes the page is upright,
# though, and a scanned book has pages bound the other way up — those come back as
# mojibake. psm 1 runs orientation detection first and reads them correctly, at the
# cost of reordering blocks, so it is used only where psm 4 has clearly failed.
UPRIGHT_MODE = "4"
AUTO_ORIENT_MODE = "1"

# Upright pages score ~0.95 on this book and upside-down ones ~0.55, so the
# threshold sits well clear of both.
MIN_WORDLIKE_RATIO = 0.8

POLISH_LETTERS = set("abcdefghijklmnoprstuwyząćęłńóśźż")


def require_tools() -> None:
    missing = [name for name in ("pdftoppm", "tesseract") if shutil.which(name) is None]
    if missing:
        sys.exit(f"missing required tools: {', '.join(missing)}\n  brew install poppler tesseract tesseract-lang")
    langs = subprocess.run(
        ["tesseract", "--list-langs"], capture_output=True, text=True, check=True
    ).stdout.split()
    if "pol" not in langs:
        sys.exit("tesseract has no Polish model\n  brew install tesseract-lang")


def page_count(pdf: Path) -> int:
    out = subprocess.run(["pdfinfo", str(pdf)], capture_output=True, text=True, check=True).stdout
    for line in out.splitlines():
        if line.startswith("Pages:"):
            return int(line.split(":", 1)[1])
    raise RuntimeError(f"no page count for {pdf}")


def wordlike_ratio(text: str) -> float:
    """How much of a page reads as Polish words rather than OCR noise."""
    tokens = [t.strip(".,:;!?()[]„”\"'-–—…") for t in text.split()]
    tokens = [t for t in tokens if len(t) >= 3]
    if not tokens:
        return 0.0
    wordlike = sum(1 for t in tokens if all(c.lower() in POLISH_LETTERS for c in t))
    return wordlike / len(tokens)


def read_image(image: Path, mode: str) -> str:
    return subprocess.run(
        ["tesseract", str(image), "-", "-l", "pol", "--psm", mode],
        capture_output=True,
        text=True,
        check=True,
    ).stdout


def ocr_page(pdf: Path, page: int, workdir: Path) -> str:
    stem = workdir / f"p{page:04d}"
    subprocess.run(
        ["pdftoppm", "-r", str(RENDER_DPI), "-gray", "-png", "-f", str(page), "-l", str(page), str(pdf), str(stem)],
        check=True,
        capture_output=True,
    )
    images = sorted(workdir.glob(f"{stem.name}-*.png"))
    if not images:
        return ""

    text = read_image(images[0], UPRIGHT_MODE)
    if wordlike_ratio(text) < MIN_WORDLIKE_RATIO:
        rotated = read_image(images[0], AUTO_ORIENT_MODE)
        if wordlike_ratio(rotated) > wordlike_ratio(text):
            text = rotated

    for image in images:
        image.unlink()
    return text


def ocr_book(book: str, force: bool) -> Path:
    pdf = book_path(book)
    target = OCR_DIR / f"{book}.txt"
    if target.exists() and not force:
        print(f"{book}: cached ({target})")
        return target

    total = page_count(pdf)
    OCR_DIR.mkdir(parents=True, exist_ok=True)
    partial = target.with_suffix(".txt.partial")
    with tempfile.TemporaryDirectory() as tmp, partial.open("w", encoding="utf-8") as out:
        workdir = Path(tmp)
        for page in range(1, total + 1):
            out.write(ocr_page(pdf, page, workdir))
            out.write("\f")
            out.flush()
            print(f"{book}: page {page}/{total}", flush=True)
    partial.replace(target)
    print(f"{book}: wrote {target}")
    return target


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("books", nargs="*", default=["a2_coursebook", "a1_workbook"])
    parser.add_argument("--force", action="store_true", help="re-run even if a cached transcript exists")
    args = parser.parse_args()

    require_tools()
    if not KROK_ROOT.exists():
        sys.exit(f"source folder not found: {KROK_ROOT}")

    for book in args.books:
        ocr_book(book, args.force)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
