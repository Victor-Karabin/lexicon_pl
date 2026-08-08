# Vocabulary presets

A **preset** is a curated collection of vocabulary grouped by topic, frequency band or CEFR
level — "Food", "100 most common words", "A2". Presets are data, not code: adding one means
editing a text file and re-running a build script, never touching Kotlin.

The browser lives in the **Vocabulary** tab.

## Architecture

Presets follow the project's existing layering. Each layer knows only the one below it.

| Layer | Package | Holds |
| --- | --- | --- |
| Presentation | `com.lexicon.presentation.presets` | `VocabularyScreen`, `PresetDetailScreen`, their ViewModels and UI state |
| Interactor | `com.lexicon.interactors.presets` | `VocabularyPreset`, `PresetCategory`, `CefrLevel`, use-case contracts |
| Domain | `com.lexicon.domain.presets` | Use-case implementations, mappers, `VocabularyPresetValidator` |
| Boundary | `com.lexicon.boundary` | `VocabularyPresetRepository`, `VocabularyPresetBoundary` |
| Data | `com.lexicon.data.local` / `.repository` | Asset DTOs, `VocabularyPresetAssetLoader`, repository implementation |

Domain and interactors stay pure Kotlin. `kotlin.time.Duration` and `ImmutableList` are the
only non-stdlib types they use.

Two deliberate consequences of the model:

- **A preset holds vocabulary ids, not words.** Listing, searching and sorting 77 presets
  never touches the word store. Words are resolved only when a preset is opened or trained
  on. Without this, the 1000-word presets would duplicate most of the corpus in memory.
- **Localized text is resolved at the edge**, by `LocalizedText.resolve(languageTag)`, not
  at load time — so the display language can change without reloading the catalogue.

`GetPresetCategoriesUseCase` currently has no caller: categories still order the preset list
and label each card, but nothing lists them on their own since the category chips were removed.
It is kept rather than deleted because it is correct and small, unlike the browse use case,
which had become a filter that could never match.

## Preset format

The app reads two generated assets from `data/src/main/assets/`:

- `vocabulary_pl.json` — every word, with `id`, `text`, `translation`, `transcription`,
  `partOfSpeech`, `cefr`, `topics`.
- `vocabulary_presets.json` — `{ "categories": [...], "presets": [...] }`.

A preset entry:

```json
{
  "id": "food",
  "category": "everyday-life",
  "title":       { "en": "Food", "pl": "Jedzenie" },
  "description": { "en": "Meals, ingredients, fruit and vegetables.", "pl": "..." },
  "icon": "restaurant",
  "color": "#EF6C00",
  "cefr": null,
  "popularity": 17,
  "estimatedSeconds": 3480,
  "wordCount": 58,
  "vocabularyIds": [168, 169, 180]
}
```

Every field except `id` and `category` is optional when parsing: a file written by an older
or newer app version still loads. Unknown fields are ignored, so a future `premium` or
`packUrl` will not break older clients.

## Authoring: adding a new preset

Sources live in `tools/vocabulary/`. Nothing here ships; it generates what ships.

```
tools/vocabulary/
  corpus/core.tsv        frequency-ordered core list — line order IS the rank
  corpus/topics/*.tsv    topical vocabulary beyond the core list
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
| `frequency` | a count, e.g. `250` | the N most frequent words in `core.tsv` |
| `cefr` | `A1`…`C2` | every word tagged with that level |
| `topic` | a tag, e.g. `food` | every word carrying that topic tag |

To add **vocabulary**, append to `corpus/core.tsv` (if it belongs in the frequency ranking)
or a file under `corpus/topics/`, then rebuild. Columns are `polish, english, pos, cefr,
topics`. Transcriptions are generated — never write IPA by hand.

The one thing that can still need a code change is an **icon**: Compose cannot look an icon
up by name, so `PresetIcons.kt` maps names to vectors. An unmapped name falls back to a
generic icon rather than failing, so a new preset always renders; add a mapping only if you
want a specific icon that nothing else uses.

## Validation

Two layers, deliberately duplicated because they protect against different things.

`build_assets.py` validates **before writing**, and a failure stops the build. It rejects
duplicate ids, unknown categories, empty presets, repeated words within a preset, unknown
parts of speech or CEFR levels, duplicate word/translation pairs across the corpus, and
presets too small to train on (fewer than six words, which Image Test needs for options).

`VocabularyPresetValidator` (domain) checks the same rules **at runtime**, and exists for
catalogues that did not come from the build tool — imported files, downloaded packs,
user-created presets. It returns every issue rather than throwing on the first, so a bad
import can be explained in full. Issues are typed (`PresetValidationIssue`), not strings.

`VocabularyPresetAssetTest` re-checks the shipped asset from the test suite, because the
build tool cannot stop the file being hand-edited afterwards.

## Repository responsibilities

`VocabularyPresetRepository` answers three questions — all presets, one preset by id, all
categories — and says nothing about where they come from. `VocabularyPresetRepositoryImpl`
reads the bundled asset, parses it once and caches it behind a mutex: the asset holds every
preset's full id list, so a concurrent burst at startup must not each trigger their own parse.

The repository does **not** validate, sort, filter or localize. Those belong to the domain
layer, so that every source gets the same treatment.

## Extension points

The architecture was shaped so these need no structural change:

| Future | How it fits |
| --- | --- |
| User-created presets | Another `VocabularyPresetRepository`, composed with the bundled one |
| Downloadable packs | Same — a repository reading from cache storage instead of assets |
| AI-generated / imported | Same, plus `VocabularyPresetValidator` on the untrusted input |
| Community presets | Same, plus whatever metadata fields; unknown fields already parse |
| Premium presets | An added field on the asset DTO; old clients ignore it |

The one assumption to preserve: a preset references vocabulary **by id**. A source that
brings its own words must first insert them into the vocabulary store and reference the
resulting ids, rather than embedding words in the preset.

## Favourites — the study set

A word can be marked with a heart, on its own row in the detail screen, from search results,
or in bulk from a preset's heart. **Trainings draw from the favourited words and nothing
else.** A user who has favourited nothing therefore has nothing to train on, which is what
`TrainingGate` exists to explain rather than leave as an empty session.

A preset's heart is tri-state — `NONE`, `SOME`, `ALL` — because a preset can be partly
favourited and a boolean would have to lie about that. Partly-favourited counts as off, so
one tap completes the preset rather than clearing it. The bulk toggle writes every word in a
single call; word by word, a thousand-word preset would emit a thousand updates.

Two consequences worth knowing:

- Favouriting **very few** words no longer degrades trainings silently. Every training is
  fronted by `TrainingGate`, which checks the study-set size against that training's minimum
  (`TrainingRequirements`) and shows a "not enough words" screen naming both numbers instead
  of starting a session it cannot build. Before this, an Image Test with three favourites ran
  with three options, and a training with none spun forever — `openStep(0)` returns early on
  an empty session, so the screen never left Loading.
- `getRandomForStudy` is plain SQL, and the project has no Robolectric or instrumentation
  setup, so **the study-set query itself is not covered by a unit test**. Everything above it
  — the use cases, the tri-state derivation, the sorting — is.

## Word search

The Vocabulary tab has one search box and one row of filter chips, and both select **words**.
With the box empty and no level picked, the preset list is shown; typing — or picking a CEFR
level — replaces that list with the matching words, and clearing both puts the presets back
exactly as they were.

Presets are not filtered, searched or sorted from the UI: the catalogue is 72 items whose
names are on screen to scan. `BrowseVocabularyPresetsUseCase` was removed once every one of
its narrowing parameters had become unreachable; `GetVocabularyPresetsUseCase` returns the
list in category-then-popularity order, which is the order shown.

**CEFR is a property of a word, not of a preset.** There are no A1/A2/B1 presets; instead
every word carries its level, and the level chips list every word at the levels picked. Levels
and the query narrow together.

Matching is by either language — "apple", "jabłko" and "jablko" all find the same entry — and
each result carries the same heart as the preset detail screen, so search is also how you add
a single word to the study set.

Presets are narrowed by the filter chips rather than by typing. A preset list is 77 items with
names you can see; a vocabulary is 1,767 words you cannot, so the box is worth more pointed at
the words.

Matching is done in SQLite against a stored `searchKey` column holding both languages folded
together (lower case, Polish diacritics stripped). The alternative — folding every row at
query time — would mean scanning the whole table per keystroke.

The folding lives in `common.foldForSearch`, used by the stored key, the query, and preset
search. That shared location is the point: a key folded one way and a query folded another
never match, and the failure is silent.

Rows carried across `MIGRATION_4_5` arrive with an empty key and are backfilled by
`VocabularySeeder` on next use. Backfilling rather than reseeding is deliberate — the rows now
carry the user's favourites, which a reseed would discard.

## Consuming presets

`GetPresetVocabularyUseCase` is the integration point. It returns a preset's words in the
preset's own order (so "100 most common words" arrives in frequency order) and silently
skips ids the store no longer holds, so a preset built against an older corpus stays usable.

Trainings, Mix, Custom Builder, Search, Favourites and any future spaced-repetition
scheduler consume presets through that one call. **Not yet wired:** no training currently
takes a preset as a session filter — that means threading a preset id through each
training's session-start request, and is separate work.

## Dataset provenance and limits

The corpus is roughly 1,770 entries: about 1,030 frequency-ranked core words plus topical
vocabulary. Two honest caveats:

- **The frequency ranking is a curated approximation**, not a corpus-derived list. It is
  ordered to be defensible for learners, but the exact rank of any given word is an
  editorial judgement, not a measurement.
- **Transcriptions are rule-generated** by `g2p.py` from spelling. Polish orthography is
  close to phonemic so this is accurate for the ordinary cases it covers — digraphs,
  softening, final devoicing, voicing assimilation, penultimate stress — but it does not
  handle loanwords whose pronunciation genuinely diverges from their spelling.
  `test_g2p.py` pins the rules against known words.
