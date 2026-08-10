"""Locations of the Krok po kroku source material.

The books live outside the repository, next to it, because they are commercial
coursebooks and must not be committed. Everything derived from them lands in
KROK_ROOT/.cache so a clean checkout never carries book content.
"""

from __future__ import annotations

import subprocess
import sys
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
KROK_ROOT = REPO_ROOT.parent / "krok"
CACHE_DIR = KROK_ROOT / ".cache"
OCR_DIR = CACHE_DIR / "ocr"
TEXT_DIR = CACHE_DIR / "text"
AUDIO_DIR = CACHE_DIR / "audio"

ASSET_DIR = REPO_ROOT / "data" / "src" / "main" / "assets"
COURSE_ASSET = ASSET_DIR / "course_krok.json"

TEXTBOOK_ZIP = "Krok_po_Kroku_Polski_A1_A2_textbooks.zip"

BOOK_SOURCES = {
    "a1_coursebook": "Iwona Stempek - Krok po Kroku Polski A1 - 2010.pdf",
    "a2_coursebook": f"{TEXTBOOK_ZIP}!Iwona Stempek - Krok po Kroku Polski A2 - 2012.pdf",
    "a1_workbook": "Krok_po_Kroku_1_Zeszyt_cwiczen/Krok po Kroku 1. Zeszyt cwiczen.pdf",
}

AUDIO_ARCHIVES = {
    "a1_coursebook": "Audio_A1.docx",
    "a2_coursebook": "Audio_A2.docx",
    "a1_workbook": "krok_po_kroku_1_wb_Audio.docx",
}


def book_path(book: str) -> Path:
    """Absolute path to a book's PDF, unpacking it from the textbook zip on demand."""
    try:
        source = BOOK_SOURCES[book]
    except KeyError:
        sys.exit(f"unknown book '{book}'; known: {', '.join(sorted(BOOK_SOURCES))}")

    if "!" not in source:
        path = KROK_ROOT / source
        if not path.exists():
            sys.exit(f"missing source PDF: {path}")
        return path

    archive_name, member = source.split("!", 1)
    extracted = CACHE_DIR / "books" / Path(member).name
    if not extracted.exists():
        archive = KROK_ROOT / archive_name
        if not archive.exists():
            sys.exit(f"missing archive: {archive}")
        extracted.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(archive) as zf, extracted.open("wb") as out:
            out.write(zf.read(member))
    return extracted


def is_ocr_source(book: str) -> bool:
    """Whether a book's text comes from tesseract rather than the PDF's own layer.

    It decides which parser can be used: OCR loses the horizontal positions that
    the laid-out parser relies on.
    """
    return (OCR_DIR / f"{book}.txt").exists()


def pdf_text(book: str) -> str:
    """Page text for a book, preferring a cached OCR transcript over the embedded layer."""
    ocr = OCR_DIR / f"{book}.txt"
    if ocr.exists():
        return ocr.read_text(encoding="utf-8", errors="replace")

    cached = TEXT_DIR / f"{book}.txt"
    if not cached.exists():
        cached.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["pdftotext", "-layout", str(book_path(book)), str(cached)],
            check=True,
            capture_output=True,
        )
    return cached.read_text(encoding="utf-8", errors="replace")


def pages(book: str) -> list[str]:
    return pdf_text(book).split("\f")
