# Courses (Plan tab)

The Plan tab is where the learner's courses are. Its first tile is the **vocabulary course**,
which runs on the dashboard and has its settings here: new words a day, reviews a day and the
queue of trainings it cycles through. Under *Courses* come the **Polish for Beginners 1**
course (*Polski dla początkujących 1*), and one tile each for the Verb Conjugation, Cases,
Numbers and Countries courses; the last three have their own pages, [Cases](cases.md),
[Numbers](numbers.md) and [Countries](countries.md).

Polish for Beginners is the lesson sequence of an A1 coursebook and its workbook — the book's
name appears nowhere in the app. Where the Words tab lets a learner pick any preset, the
course answers "what should I study next".

A lesson carries its title, the words it introduces, the recordings that go with its
exercises and, for lessons 1 to 9, a guided script. A lesson without a script lists the
book's exercises, each opening on its own screen. Every lesson is open from the start.

## Not distributable

The lesson text, titles and word lists are taken from commercial coursebooks. The
resulting APK must not be distributed. Everything book-derived is confined to:

- `tools/course/` — the extraction scripts
- `data/src/androidMain/assets/course_krok.json` — the generated catalogue, and
  `lesson_krok-a1-01.json` to `lesson_krok-a1-09.json`, the guided scripts
- `krok/` — the books themselves, which live outside the repository and are never
  committed

A shippable variant would drop those and take the course step out of the startup seeding
(`SeedCatalogsUseCase`), which treats it like its other steps.

## What the sources actually give us

| Source | Text quality | What we take | Shipped |
| --- | --- | --- | --- |
| A1 coursebook | native PDF text, exact | 26 lessons: title, syllabus, new words, audio tags | yes, except the syllabus |
| A2 coursebook | 155-page scan, re-OCR'd | 23 lesson titles; word lists unreliable | **no — see below** |
| A1 workbook | image-only scan | audio only | audio only |

The A2 coursebook ships with an embedded OCR layer from I.R.I.S. that replaces
about one letter in ten with a Cyrillic look-alike (`о` for `o`, `р` for `p`), so
it is unusable as-is and is re-read with tesseract instead. Some of its pages are
bound the other way up; `ocr_books.py` detects those by how little of the page
reads as Polish and re-runs them with orientation detection.

Audio: **488 tracks, 454 MB** — 220 with the A1 coursebook, 180 with A2, 88 with
the A1 workbook.

### Why A2 is not shipped

`extract_krok.py` recovers all 23 A2 lessons and their titles, and the ordering is
corroborated by the A2/B1 workbook's own table of contents. The word lists are the
problem: they are printed in coloured bold on a coloured band, which the scan reads
badly. Headwords come back misspelled — `niepefnosprawny`, `zatoba`, `repreSje`,
`tryb dorazny` — and only **35 of 222** match the corpus.

Authoring English glosses on top of misspelled Polish would teach the misspelling,
which is worse than having no A2. So A2 is gated off in `COURSES` in
`build_course.py` until its word lists are transcribed. That work is mechanical and
already has a home: lessons 6, 12, 19 and 21 are done this way in
`lesson_overrides.tsv`, transcribed from the rendered page band. The remaining 19
lessons need the same treatment, then their new words authored into
`zz-krok.tsv`.

## Pipeline

Mirrors `tools/vocabulary/`: sources in, validation, one asset out, nothing
generated at runtime.

```bash
brew install poppler tesseract tesseract-lang
python3 tools/course/ocr_books.py          # once, ~45 min: A2 + A1 workbook
python3 tools/course/extract_audio.py      # audio manifest
python3 tools/course/extract_krok.py       # lesson structure
python3 tools/course/fetch_drive_manifest.py   # Google Drive ids of the recordings
python3 tools/course/build_course.py       # -> data/src/androidMain/assets/course_krok.json
```

The guided scripts are not generated: `lesson_krok-a1-NN.json` is written by hand beside the
catalogue, and `build_course.py` reads it to link the phrases its vocabulary tables teach.

`ocr_books.py` caches to `krok/.cache/ocr/`, so it only runs once; the others are fast and
safe to re-run.

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

The coursebook is monolingual. Lesson words are matched to the existing corpus by
Polish form, folded through the same `foldForSearch` the app uses. Whatever does
not match has to be authored:

```bash
python3 tools/course/build_course.py --report-missing
```

New entries go into `tools/vocabulary/corpus/topics/zz-krok.tsv` — an ordinary
topic file, so they pick up transcriptions from `g2p.py` and ids from
`build_assets.py` for free. Ids are handed out in load order, so a file sorting
earlier than an existing one would renumber every word after it; the phrases a guided
lesson's vocabulary tables teach go in `zzzzzz-krok-lessons.tsv`, which sorts last.

Forms the books print with a conjugation (`spać (śpię, śpisz)`), an aspect partner
(`zamawiać/zamówić`) or in the gender a dialogue happens to use (`marudna`) are
mapped back to their headword by `tools/course/word_forms.tsv`.

## Runtime

`course_krok.json` is imported into Room on first launch by `CourseSeeder`,
fingerprint-guarded exactly like `VocabularyPresetSeeder`, and appears as a third
step on the splash screen. A build without `course_krok.json` reads it as an empty
catalogue: the step finishes with 0 lessons and the app opens without the course.
Everything after that reads the database, not the asset. The guided scripts are not imported: a lesson's script is read from its
`lesson_<id>.json` asset when the lesson opens.

The catalogue is replaced wholesale whenever the asset changes. The progress tables
(`lesson_progress`, `lesson_script_progress`, `lesson_last_results`) and
`dashboard_courses` are separate and deliberately not part of that replace: they are the
learner's, not the book's.

There is no lock on the order: every lesson is open from the start. Finishing a guided
lesson marks it done (`SetLessonCompletedUseCase`), and the lesson list can reset a
lesson, clearing its answers, scores and done mark. The course tile can be pinned to the
dashboard.

### The guided lessons

A lesson with a script (`LessonScript`) runs as a flow of screens rather than a list of
exercises, and the lesson page offers *Start lesson* or *Continue lesson* where the exercise
list would be.

- The script is a sequence of **steps**, each a run of **screens**; a step can be a review
  of the whole lesson or a grammar step. A screen has one of several shapes: reference
  tables and phrases, grammar, typed answers, choices, a choice inside a line of dialogue,
  ordering lines, a form, a verb table, filling gaps, a selection grid, or free writing.
- A screen can carry recordings (see *Audio*), a transcript that opens always, after the
  check or after another screen, and links to the grammar rules it practises. Each step
  can list its new words.
- Answers are checked by `LessonAnswerChecker`. A screen can be checked once every answer is
  filled in; an exercise scored below the pass mark in Settings (80% by default) starts
  again and keeps the right answers. At 0% it never repeats.
- Free writing is read by OpenAI against a model answer and a checklist
  (`WritingReviewer`); offline, or when the check is refused, the learner is shown the
  model answer to compare with.
- Progress is saved by screen, so *Continue lesson* resumes where the learner stopped.
  The results screen scores each step, names what to work on and the rules to revisit,
  and says how many answers were right on the first try.

### Scoped trainings

Every `Start*SessionRequest` takes an optional `vocabularyIds`; empty means the
whole study set, which is what the Trainings tab passes. Only the vocabulary course fills
it in, and it travels as a `?words=` argument on the training route. A scoped session
trains exactly the words it is given, whatever their status.

When the vocabulary course launches Fillword, Passage or Sentences, they use the
course's words too. Fillword packs the course words that fit the grid, and each
placed word is recorded when the round ends, on the last word found or on Check:
found is correct, not found is a lapse. Leaving mid-round records nothing. Passage
and Sentences write sentences only for the course words and are never topped up
from outside them. Passage asks for a couple of spare sentences, so a refused one
is replaced by a spare course word while any is left; Sentences has no spares, so
a sentence that does not come back leaves one step fewer. Fewer course words than
the step count gives fewer steps in both.

Sentences are practice for saying a whole sentence, not for knowing the word: an
answer is kept in the history and the day's accuracy, but it does not move the
word's review schedule, does not count as a new word, and cannot make a word known.

### Which words the vocabulary course runs

The words being learnt (to learn and favourite, favourites first, then in word order) are
always in, up to the day's number of new words. The reviews, up to the day's number, are
known words chosen by the review schedule: those already reviewed today first, then the due
ones, most overdue first, then, if the day's number is not yet reached, known words that
have no schedule yet, in word order. The choice is never random; the session only deals
the chosen words in a shuffled order. A word reviewed today stays in the course for the
rest of the day, so a round does not change under the learner. Any answer that moves a
word's schedule counts toward that, whichever training it came from, including one given
outside the course. When nothing is being learnt, nothing is due and no known word is
waiting without a schedule, the course has nothing to launch; a training that needs more
words than the session has is skipped for the next one in the queue.

Only a word's first answer of the day moves its review schedule. Later answers the same
day, right or wrong, go to the history, the day's accuracy and the study record and
leave the schedule where it is. A word marked to learn or favourite becomes known the
first time one of those schedule-moving answers brings its review interval to the
mastery threshold (21 days), and a later lapse does not take it back.

The Home card shows the counts as *N to learn · M known*, followed by *· K due* when any
known word is due and the day's accuracy once there is an answer to measure.

### Audio

454 MB does not belong in an APK. The app downloads a recording from Google Drive the first
time it is played, using the file id the catalogue records for it, and keeps it in the
app's own external files directory (`lesson_audio`), where `LessonAudioLibrary` looks
first. To have every track at once, side-load them:

```bash
./tools/course/install_audio.sh
```

A recording that is not on the device and cannot be downloaded shows only the plain
player: no label, no disabled state and no error message.

## Caveats

- **Only A1 ships**, for the reason above. The Plan tab shows 26 lessons, not 49.
- **A2 has no syllabus or section data** even once its words are transcribed — the
  scan does not preserve the column layout that makes those recoverable in A1.
- **A2 lesson numbering comes from opener order.** `extract_krok.py` fails the
  build if it does not find exactly 23 openers, which is what makes the ordering
  safe to trust.
- **The A1 workbook contributes audio only.** Its PDF has no text layer at all.
- English glosses in `zz-krok.tsv` are authored, not sourced, so they give the
  usual sense of the word rather than the one the lesson intends.
