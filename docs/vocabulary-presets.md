# Vocabulary presets

A **preset** is a curated collection of vocabulary grouped by topic or frequency band —
"Food", "Top 100". Presets are data, not code: adding a shipped one means editing a text file
and re-running a build script, never touching Kotlin. The learner can also make presets of
their own, which live beside the shipped ones.

The browser lives in the **Words** tab.

## Architecture

Presets follow the project's existing layering. Each layer knows only the one below it.

| Layer | Package | Holds |
| --- | --- | --- |
| Presentation | `com.lexicon.presentation.presets` | `VocabularyScreen`, `PresetDetailScreen`, `CreatePresetScreen`, `CreateWordScreen`, their ViewModels and UI state |
| Interactor | `com.lexicon.interactors.presets` | Use-case contracts: `GetVocabularyPresetsUseCase`, `ObserveVocabularyPresetsUseCase`, `GetPresetVocabularyUseCase`, `CreatePresetUseCase`, `SetWordPresetMembershipUseCase` and the rest |
| Model | `com.lexicon.model.vocabulary` | `VocabularyPreset`, `PresetCategory`, `PresetId`, `LocalizedText`, `Word`, `WordStatus`, `CefrLevel` |
| Application | `com.lexicon.application.presets` | Use-case implementations and mappers |
| Boundary | `com.lexicon.boundary` | `VocabularyPresetRepository`, `VocabularyPresetBoundary` |
| Data | `com.lexicon.data.local` / `.repository` | Asset DTOs, `VocabularyPresetAssetLoader`, `VocabularyPresetSeeder`, `VocabularyPresetRepositoryImpl` |

The model stays pure Kotlin: beyond the standard library (`kotlin.time.Duration` among it)
its only dependency is `ImmutableList`.

Two deliberate consequences of the model:

- **A preset holds vocabulary ids, not words.** Listing and sorting the presets never
  touches the word store. Words are resolved only when a preset is opened. Without this,
  the 3,000-word Top 3000 preset alone would copy a good part of the corpus into memory.
- **Localized text is resolved at the edge**, by `LocalizedText.resolve(languageTag)`, not
  at load time — so the display language can change without reloading the catalogue.

Categories decide the order of the list and nothing else: category order, then popularity,
which is the order the Words tab shows. Nothing on screen names a category.

## Preset format

The assets are the **build-time source**, not the runtime one. Both are imported into the
database at startup, and everything afterwards reads rows — see *Startup sync* below.

The app reads two generated assets from `data/src/androidMain/assets/`:

- `vocabulary_pl.json` — every word, with `id`, `text`, `translation`, `transcription`,
  `partOfSpeech`, `cefr`, `topics`, and where they exist `example`, `picture`, `gender`,
  `declension` and `forms`.
- `vocabulary_presets.json` — `{ "categories": [...], "presets": [...] }`.

A preset entry (the first three ids of `food`, which lists 317):

```json
{
  "id": "food",
  "category": "everyday-life",
  "title":       { "en": "Food", "pl": "Jedzenie" },
  "description": { "en": "Meals, ingredients, fruit and vegetables.", "pl": "Posiłki, składniki, owoce i warzywa." },
  "icon": "restaurant",
  "color": "#EF6C00",
  "popularity": 17,
  "estimatedSeconds": 19020,
  "wordCount": 317,
  "vocabularyIds": [140, 141, 142]
}
```

Every field except `id` and `category` is optional when parsing: a file written by an older
or newer app version still loads. Unknown fields are ignored, so a future `premium` or
`packUrl` will not break older clients.

## Authoring: adding a new preset

Sources live in `tools/vocabulary/`. Nothing here ships; it generates what ships.

```
tools/vocabulary/
  corpus/core.tsv        the core list, loaded first — its line order gives the first ids
  corpus/topics/*.tsv    every other entry, loaded in file-name order
  frequency.tsv          KWJP frequency per entry, written by build_frequency.py
  categories.tsv         preset categories
  presets.tsv            preset metadata and selection rules
  g2p.py                 Polish spelling → IPA
  build_assets.py        validates, then writes both assets
```

To add a preset, append a row to `presets.tsv` and run:

```bash
python3 tools/vocabulary/build_assets.py
```

Columns are `id, category, select_type, select_arg, popularity, icon, color, title_en,
title_pl, desc_en, desc_pl`. The selection rule decides membership, so no id list is ever
written by hand:

| `select_type` | `select_arg` | Selects |
| --- | --- | --- |
| `frequency` | a count, e.g. `250` | the N entries with the best frequency rank, from `frequency.tsv`; an entry KWJP never saw has no rank and is never selected |
| `topic` | a tag, e.g. `food` | every word carrying that topic tag |
| `pos` | a part of speech, e.g. `expr` | every word of that kind — how the Phrases preset is built |

To add **vocabulary**, add rows to a file under `corpus/topics/`, then rebuild. Columns are
`polish, english, pos, cefr, topics`. Ids are handed out in load order — `core.tsv`, then the
topic files sorted by name — so a new entry belongs at the end of the last file, or in a new
file whose name sorts after the existing ones; anywhere earlier renumbers every word after
it, and with it the ids that presets, lessons and a learner's marks point at. Transcriptions
are generated — never write IPA by hand.

The one thing that can still need a code change is an **icon**: Compose cannot look an icon
up by name, so `PresetIcons.kt` maps names to vectors. An unmapped name falls back to a
generic icon rather than failing, so a new preset always renders; add a mapping only if you
want a specific icon that nothing else uses.

## Validation

Two layers, deliberately duplicated because they protect against different things.

`build_assets.py` validates **before writing**, and a failure stops the build. It rejects an
empty word or translation, an unknown part of speech or CEFR level, an entry shorter than
three letters, a duplicate word/translation pair across the corpus, an unknown category,
duplicate word or preset ids, a preset naming a word that does not exist, an empty preset,
a word repeated within a preset, a preset too small to train on (fewer than six words, which
Image Test needs for options), Cyrillic letters in any text field, and a CEFR level with no
words. `test_build_assets.py` pins the validator against small synthetic corpora.

`VocabularyPresetAssetTest` re-checks the shipped asset from the unit-test suite, because the
build tool cannot stop the file being hand-edited afterwards: the catalogue must parse, ids
must be unique, every preset must name a known category, have an English title and at least
one word, list only words that exist, and list none twice; every CEFR level must have words,
and no entry may be shorter than three letters.

There is no validator at runtime. The app trusts the asset it ships, and the one draft it
accepts from the learner, a new preset, is checked for a title (`PresetDraftProblem`).

## Startup sync

`SeedCatalogsUseCase` runs on the splash screen and reports each step as it goes, because on
a first launch it writes thousands of rows and a splash that says nothing for that long is
indistinguishable from one that has hung. The screen shows, per step, whether it is waiting,
importing, already current with a count, or failed with a reason. The steps are the
vocabulary, the presets, the course lessons and the verbs.

When the installed app version matches the one last seeded (`CatalogSeedGate`) and the word
table has rows, the whole step list is reported as current without reading any asset.

Vocabulary is seeded first, and the other steps are skipped if it fails with nothing to fall
back on: a preset is a list of word ids, so importing one over an empty vocabulary produces a
catalogue whose every entry resolves to nothing.

The two stores are kept current differently, and the difference is the point:

| | Vocabulary | Presets |
| --- | --- | --- |
| Strategy | reconcile row by row | replace wholesale, then put back what the learner owns |
| Why | rows carry the user's word statuses, deletions and own words, which are not the asset's to overwrite | the shipped list is the asset's, but the learner's own presets, their deletions of shipped ones and their adding and removing of words are re-applied afterwards |

Both skip their work when a fingerprint of the asset matches the last seeded value, so an
unchanged asset costs a file read and no JSON parse. An empty table is still seeded
whatever the fingerprint says, because a schema change can empty a table without the asset
moving at all.

A failed seed blocks startup only when it leaves the app with nothing usable. Over a store
that already holds rows it is a stale catalogue, not a broken app, and stranding the user on
a splash screen over something they cannot fix would be the worse outcome.

## Presets the learner owns

- **My presets.** A learner can create a preset (`CreatePresetUseCase`). It is stored in the
  same tables, flagged `isUserCreated`, under the category `my-presets`, which sorts before
  the shipped ones. A re-seed keeps it.
- **Unsorted.** A synthetic preset, `unsorted`, sorts first and lists the words that are in
  no other preset. It is built on read, cannot be deleted or edited, and a word leaves it
  when it joins a preset.
- **Changing a word's presets.** A word can be added to or removed from any preset
  (`SetWordPresetMembershipUseCase`). Each change is also recorded as an override
  (`preset_word_overrides`), which the seeder re-applies after replacing the shipped
  catalogue, so a hand-made membership survives an app update.
- **Deleting a preset.** The preset and its memberships go, the words stay. The
  preset's id is recorded in `deleted_presets` so a re-seed does not bring a shipped one
  back, and restoring it (the undo) clears that record and seeds again.

## Repository responsibilities

`VocabularyPresetRepository` seeds the catalogue from its asset (`seedFromAsset`), lists and
observes presets, returns one preset or the categories, creates, deletes and restores a
preset, and reads or changes which presets a word belongs to. It says nothing about where
the presets come from. `VocabularyPresetRepositoryImpl` reads them from the database.
Memberships are fetched in one query and grouped in memory rather than one query per preset,
which would be a round trip for each of the 75 shipped presets to draw one screen.

The repository does **not** validate, sort, filter or localize, apart from adding the
synthetic Unsorted preset to what it returns. Those belong to the application layer, so that
every source gets the same treatment.

## Extension points

The architecture was shaped so these need no structural change:

| Future | How it fits |
| --- | --- |
| Downloadable packs | A repository reading from cache storage instead of assets |
| AI-generated / imported | Same, plus a runtime check of the untrusted input — the build tool's rules live in Python, so this would be new code |
| Community presets | Same, plus whatever metadata fields; unknown fields already parse |
| Premium presets | An added field on the asset DTO; old clients ignore it |

The one assumption to preserve: a preset references vocabulary **by id**. A source that
brings its own words must first insert them into the vocabulary store and reference the
resulting ids, rather than embedding words in the preset.

## Word statuses and the study set

Every word has a status: not defined, which is where every word starts, then to learn,
favourite and known. One tap on a word's status button — in a preset, in search results, in a
lesson — moves it to the next, and after known it goes back to not defined. The **study set**
is the words marked to learn or favourite, and **trainings draw from the study set and nothing
else**, except where a session is handed its own words, as the vocabulary course does. A user
with an empty study set therefore has nothing to train on, which is what `TrainingGate` exists
to explain rather than leave as an empty session.

Presets carry no status of their own. The preset heart went with the move to four states: a
mark on a whole preset had no single meaning once a word could be known as well as starred.
*Review words* on the dashboard is the quick way through many words — it deals the ones with
no status yet one card at a time, with a button for each status, and one to delete the word
when the learner made it.

Two consequences worth knowing:

- A study set of **very few** words no longer degrades trainings silently. Every training is
  fronted by `TrainingGate`, which checks the study-set size against that training's minimum
  (`TrainingType.minimumWords`) and shows a "not enough words" screen naming both numbers instead
  of starting a session it cannot build. Before this, an Image Test with three marked words ran
  with three options, and a training with none spun forever — `openStep(0)` returns early on
  an empty session, so the screen never left Loading.
- The status queries are plain SQL, covered by the instrumented `WordDaoTest`, which needs a
  device or emulator. Everything above them — the use cases, the status cycle, the sorting —
  has unit tests.

## Word search

The Words tab has one search box and one row of filter chips, and both select **words**.
With the box empty and no filter picked, the preset list is shown; typing — or picking a
CEFR level or the *To learn* chip — replaces that list with the matching words, forty at a
time, and clearing everything puts the presets back exactly as they were.

Presets are not filtered, searched or sorted from the UI: the catalogue is 75 shipped items
(plus Unsorted and the learner's own) whose names are on screen to scan, while the
vocabulary is 7,544 words nobody can see, so the box is worth more pointed at the words.
`ObserveVocabularyPresetsUseCase` returns the list in category-then-popularity order, which
is the order shown.

**CEFR is a property of a word, not of a preset.** There are no A1/A2/B1 presets; instead
every word carries its level, and the level chips list every word at the levels picked. Levels,
the *To learn* chip and the query narrow together.

Matching is by either language — "apple", "jabłko" and "jablko" all find the same entry — and
each result carries the same status button as the preset detail screen, so search is also how
you mark a single word. The *To learn* chip narrows the list to the study set.

Matching is done in SQLite against a stored `searchKey` column holding both languages folded
together (lower case, Polish diacritics stripped). The alternative — folding every row at
query time — would mean scanning the whole table per keystroke.

The folding lives in `common.foldForSearch`, used by the stored key and the query, and by
the Cases course's word list. That shared location is the point: a key folded one way and a
query folded another never match, and the failure is silent.

`VocabularySeeder` keeps the table in line with the asset rather than only filling an empty
one. On launch it fingerprints the asset; if that differs from the last seeded value it inserts
new words, deletes words the asset has dropped, and refreshes the rest — **preserving each
word's status and deleted mark**, which are the user's and not the asset's to state, and
leaving words the learner created alone. Unchanged assets cost a file read and no parse.

This is not an optimisation, it is the difference between shipping a corpus update and not:
before it, an install seeded once never received another word, so the words added for the
upper CEFR bands reached only fresh installs and the C2 filter stayed empty on every existing
device.

## Consuming presets

`GetPresetVocabularyUseCase` is the integration point. It returns a preset's words in the
preset's own order (so "Top 100" arrives in frequency order) and silently skips ids the store
no longer holds, so a preset built against an older corpus stays usable. The preset detail
screen is its only consumer.

**Not yet wired:** no training takes a preset as a session filter — that means threading a
preset id through each training's session-start request, and is separate work. Trainings draw
from the study set, and the vocabulary course hands each session its own word ids.

## Dataset provenance and limits

The corpus is 7,544 entries, counted as the rows of `core.tsv` and `corpus/topics/*.tsv`:
1,005 in the core list, 1,417 in the hand-written topic files, 102 false friends, 569 from the
coursebook lessons, and 4,451 added from Tatoeba's sentences and the KWJP frequency lists by
`build_new_entries.py`. By CEFR band: A1 1,728, A2 2,031, B1 1,608, B2 1,515, C1 499, C2 163.

The **Phrases** preset gathers all 892 entries tagged as phrases by part of speech rather
than by topic tag, so a phrase added to the corpus joins it without anyone remembering to
label it. Eighteen of them are single words — *dziękuję*, *przepraszam*, *dobranoc*,
*powodzenia* and *smacznego* among them — which are utterances rather than vocabulary, and
belong there for the same reason the rest do.

985 entries are multi-word, of which 874 are tagged as phrases. Phrases are excluded from
Crossword, and never mixed with single words inside one Image Test, Word Match or True or
False step, because a phrase is a different kind of answer.

Entries are at least three letters. One- and two-letter words are function words — *w*, *na*,
*że* — and they break every training built on spelling: there is no letter puzzle or crossword
answer in a two-letter preposition. `MIN_WORD_LENGTH` in the build tool rejects them.

Every band has to be non-empty, because each is offered as a filter chip and a chip that
returns nothing is a control that silently does nothing — C2 shipped that way once. Both
`build_assets.py` and `VocabularyPresetAssetTest` now refuse an empty band, and likewise a
word below the minimum length.

Three honest caveats:

- **The frequency ranking is KWJP's, by lemma.** `build_frequency.py` ranks an entry by how
  often the corpus uses its lemma, so every form of *bać* counts for *bać się*, and a phrase
  by how often that exact sequence occurs. Longer phrases and words KWJP never saw are
  unranked, which keeps them out of the Top N presets whatever their level.
- **The upper bands are editorial.** Assigning a word to B2 rather than C1 is a judgement,
  not a measurement against a syllabus; the ordering between bands is meaningful, the exact
  boundary is not.
- **Transcriptions are rule-generated** by `g2p.py` from spelling. Polish orthography is
  close to phonemic so this is accurate for the ordinary cases it covers — digraphs,
  softening, final devoicing, voicing assimilation, penultimate stress — but it does not
  handle loanwords whose pronunciation genuinely diverges from their spelling.
  `test_g2p.py` pins the rules against known words.
