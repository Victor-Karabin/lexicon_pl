# The course (Plan tab)

The Plan tab holds an ordered path through the vocabulary: the lesson sequence of
*Polski krok po kroku*. Where the Vocabulary tab lets a learner pick any preset,
the course answers "what should I study next".

A lesson carries the book's own syllabus (what it teaches you to say, its topic
vocabulary, its grammar point), the words it introduces, and the recordings that
go with its exercises. **Train this lesson** runs the normal trainings over just
those words.

## Not distributable

The lesson text, titles and word lists are taken from commercial coursebooks. The
resulting APK must not be distributed. Everything book-derived is confined to:

- `tools/course/` — the extraction scripts
- `data/src/main/assets/course_krok.json` — the single generated asset
- `krok/` — the books themselves, which live outside the repository and are never
  committed

Dropping those three leaves a working app without a Plan tab, which is what a
shippable variant would be.

## What the sources actually give us

| Source | Text quality | What we take |
| --- | --- | --- |
| A1 coursebook | native PDF text, exact | 26 lessons: title, syllabus, sections, new words, audio tags |
| A2 coursebook | 155-page scan, re-OCR'd | lesson titles and new words only |
| A1 workbook | image-only scan | audio only |

The A2 coursebook ships with an embedded OCR layer from I.R.I.S. that replaces
about one letter in ten with a Cyrillic look-alike (`о` for `o`, `р` for `p`), so
it is unusable as-is and is re-read with tesseract instead. Some of its pages are
bound the other way up; `ocr_books.py` detects those by how little of the page
reads as Polish and re-runs them with orientation detection.

Audio: **488 tracks, 454 MB** — 220 with the A1 coursebook, 180 with A2, 88 with
the A1 workbook.

## Pipeline

Mirrors `tools/vocabulary/`: sources in, validation, one asset out, nothing
generated at runtime.

```bash
brew install poppler tesseract tesseract-lang
python3 tools/course/ocr_books.py          # once, ~45 min: A2 + A1 workbook
python3 tools/course/extract_audio.py      # audio manifest
python3 tools/course/extract_krok.py       # lesson structure
python3 tools/course/build_course.py       # -> data/src/main/assets/course_krok.json
```

`ocr_books.py` caches to `krok/.cache/ocr/`, so it only runs once; the other three
are fast and safe to re-run.

### How a lesson opener is parsed

Every A1 lesson opens on a page with the same shape:

```
PIERWSZY DZIEŃ
   W SZKOLE
                          Lekcja_01
komunikacja        słownictwo         gramatyka
powitania          podstawowe zwroty  alfabet
przedstawianie się                    liczebniki 0-10

tak, nie, proszę, dziękuję, ...
                                      nowe słowa
```

`pdftotext -layout` keeps horizontal position, so the header row fixes each
column's left edge and later rows are split on their wide gutters and matched
back. Cells are not flush with their labels, so each row's runs are assigned to
columns by the order-preserving arrangement with the smallest total distance —
nearest-header alone drops two runs into one column and leaves another empty. The
blank line below the columns is what separates the syllabus from the new-word
list.

The OCR path (A2) cannot use any of that, so it takes only the title, the number
and the comma-separated word list, and numbers lessons by the order their openers
appear rather than trusting an OCR digit.

### Translations

*Krok po kroku* is monolingual. Lesson words are matched to the existing corpus by
Polish form, folded through the same `foldForSearch` the app uses. Whatever does
not match has to be authored:

```bash
python3 tools/course/build_course.py --report-missing
```

New entries go into `tools/vocabulary/corpus/topics/zz-krok.tsv` — an ordinary
topic file, so they pick up transcriptions from `g2p.py` and ids from
`build_assets.py` for free. Its name keeps it last in the glob: ids are handed out
in load order, so a file sorting earlier would renumber every word after it.

Forms the books print with a conjugation (`spać (śpię, śpisz)`), an aspect partner
(`zamawiać/zamówić`) or in the gender a dialogue happens to use (`marudna`) are
mapped back to their headword by `tools/course/word_forms.tsv`.

## Runtime

`course_krok.json` is imported into Room on first launch by `CourseSeeder`,
fingerprint-guarded exactly like `VocabularyPresetSeeder`, and appears as a third
step on the splash screen. Everything after that reads the database, not the
asset.

The catalogue is replaced wholesale whenever the asset changes. `lesson_progress`
is a separate table and is deliberately not part of that replace: it is the
learner's, not the book's.

`LessonUnlockRule` is the one place the progression lives — a lesson opens when
the one before it is complete, the first is always open, and a completed lesson
stays open so it can be revisited.

### Training a single lesson

Every `Start*SessionRequest` takes an optional `vocabularyIds`; empty means the
whole study set, which is what the Trainings tab passes. A lesson fills it in, and
it travels as a `?words=` argument on the training route. Unlike the study set,
a lesson-scoped session ignores the favourite flag: picking the lesson is already
the choice of what to practise.

### Audio

454 MB does not belong in an APK, so the tracks are side-loaded:

```bash
./tools/course/install_audio.sh
```

That pushes them to the app's own external files directory, where
`LessonAudioLibrary` looks for them. A lesson works without them — the audio chips
are simply disabled and the screen says so.

## Caveats

- **A2 has no syllabus or section data.** The scan does not support it. Its
  lessons carry a title and a word list.
- **A2 lesson numbering comes from opener order.** `extract_krok.py` fails the
  build if it does not find exactly 23 openers, which is what makes the ordering
  safe to trust.
- **The A1 workbook contributes audio only.** Its PDF has no text layer at all.
- Book-derived English glosses in `zz-krok.tsv` are authored, not sourced, so they
  are the usual sense of the word rather than the one the lesson intends.
