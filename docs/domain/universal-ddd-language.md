# Universal DDD Language

The authoritative vocabulary for Lexicon. If a term appears here, it means what this
document says it means, everywhere — in class names, use cases, tests, documentation and
user-facing copy.

This document is derived from the code as it stands, not from an idealised model. Where
the code and this document disagree, that is a defect in one of them; the
[Terminology Change History](#terminology-change-history) records which was fixed.

## Domain Overview

Lexicon teaches Polish to an English speaker. The learner keeps a **study set** of words
they want to know, practises them through **trainings**, and their answers feed a
**review schedule** that decides what comes back and when. Longer-running structures sit
above that: a **program** plans a day's work, and **courses** teach a fixed body of
material.

Everything the learner practises comes from one of three sources: the shipped
**vocabulary catalogue**, the **course** content, or the **verb catalogue**.

## Where the model lives

```
model          the domain — pure Kotlin, no framework of any kind
   ^      ^
boundary   interactors        ports        use-case contracts
   ^   ^        ^
data android  application     adapters     use-case implementations
                  ^
             presentation
```

`model` depends on nothing but the Kotlin standard library and immutable collections.
Five of the seven contexts have their model there; Conjugation and Catalogue do not yet
(see [Known gaps](#known-gaps)).

## Bounded Contexts

| Context | Owns | Model | Contracts |
| --- | --- | --- | --- |
| Vocabulary | Words, presets, the study set, images and translations | `model.vocabulary` | `interactors.presets` |
| Training | A single practice session and what it records | `model.training` | `interactors.<training>` × 13 |
| Scheduling | Review intervals, mastery, study days, streaks | `model.scheduling` | — |
| Program | The daily plan and its queue of trainings | `model.program` | `interactors.program` |
| Course | Fixed teaching material — lessons and exercises | `model.course` | `interactors.course` |
| Conjugation | Verbs, their forms, and courses over them | — | `interactors.conjugation` |
| Catalogue | Seeding shipped data into the database | — | `interactors.sync` |

**Training is one context, not thirteen.** `dictation`, `crossword`, `imagetest` and the
rest are packages of contracts for one context: every one of them opens a `Session`,
answers `Step`s, and records the same `Outcome`. The package split is a feature slice, not
a domain boundary.

**Settings is not a bounded context.** Theme, step count and voice are application
preferences, not a domain the business reasons about.

The same English word means different things in different contexts. Those cases are listed
under [Context-Specific Terminology](#context-specific-terminology) and must not be merged.

## Canonical Terms

| Term | Definition | Context | Avoid | Code |
| --- | --- | --- | --- | --- |
| Word | A Polish word or phrase with its English translation, IPA and optional picture | Vocabulary | *entry*, *item*, *term* | `Word`, `VocabularyId` |
| Study set | The words the learner has chosen to practise | Vocabulary | *favourites* | `isInStudySet`, `studySetWordIds()` |
| Preset | A named, shipped or hand-made grouping of words by topic | Vocabulary | *category* | `VocabularyPreset`, `PresetId` |
| Preset category | A grouping of presets | Vocabulary | *topic* | `PresetCategory` |
| Membership | Whether a word belongs to a preset, and how much of a preset the study set holds | Vocabulary | *link*, *relation* | `PresetStudySetState` |
| CEFR level | How hard a word is, A1 to C2 | Vocabulary | *difficulty* (that is an ordering) | `CefrLevel` |
| Training | A kind of exercise — dictation, crossword, word search | Training | *game*, *test* | `TrainingType` |
| Session | One run of one training, start to result screen | Training | *training*, *round* | `Session`, `SessionId` |
| Step | One question inside a session | Training | *item*, *card* | `Step.Question`, `Step.Board` |
| Board | A step that puts several words up to be paired rather than asking one question | Training | *step* unqualified | `Step.Board` |
| Outcome | How one step was answered, or that it was only shown | Training | *score*, *status* | `StepOutcome` |
| Training result | One recorded answer, kept for scheduling and statistics | Training / Scheduling | *history entry* | `TrainingResultBoundary` |
| Review | A later encounter with a word the learner has already met | Scheduling | *repetition* | `ReviewScheduleRepository`, `ReviewState` |
| Due | A word whose review interval has elapsed | Scheduling | *pending*, *expired* | `dueAtEpochDay`, `dueOn` |
| Recall quality | How well a word was remembered, as the scheduler grades it | Scheduling | *score* | `RecallQuality` |
| Review settings | The policy the scheduler applies: intervals, ease, mastery threshold | Scheduling | *config* | `ReviewSettings` |
| Study time policy | How much of the gap between two answers counts as studying | Scheduling | — | `StudyTimePolicy` |
| Word mastery | A word whose review interval has passed the settings' threshold | Scheduling | *learned* | `ReviewState.isMastered` |
| Study day | A calendar day on which the learner practised, with its totals | Scheduling | *session day* | `StudyDayBoundary` |
| Streak | Consecutive study days | Scheduling | — | `GetStudyStreakUseCase` |
| Program | A configuration that plans a learner's daily work | Program | *course*, *plan* | `ProgramId`, `Program` |
| Program configuration | The stored, read-mostly description of a program | Program | *settings* | `ProgramConfig` — a stored format, not a domain object |
| Enrolment | The learner's participation in a program | Program | *subscription* | `ProgramEnrolment`, `EnrolmentStatus` |
| Program day | One day's plan for a program, and how much of it is done | Program | *daily plan* | `ProgramDay` |
| Queue | The ordered trainings a program day asks for | Program | *playlist* | `QueuedTraining` |
| Activity | A unit of work in a program's plan, mapped to a training | Program | *task* | `ActivityType`, `PlannedActivity` |
| Scope | Where a program's words come from, and in what order | Program | *filter* | `ScopeSourceType`, `ScopeOrdering` |
| Progress | Configured metrics combined by weight into one figure | Program | *score* | `ProgramProgress`, `ProgressMetric` |
| Word card | A word shown for learning rather than testing, before the day's trainings | Program | *flashcard* | `WordCard` |
| Course | Fixed teaching material — a sequence of lessons | Course | *program*, *class* | `Course`, `CourseId` |
| Lesson | One unit of a course, with its words, audio and exercises | Course | *chapter*, *unit* | `Lesson`, `LessonId` |
| Exercise | A question inside a lesson, of a fixed authored shape | Course | *training*, *step* | `LessonExercise` |
| Verb | A Polish infinitive with the forms the source records for it | Conjugation | *word* | `VerbConjugation` |
| Grammatical person | One of the six persons the source distinguishes | Conjugation | *pronoun*, *form* | `GrammaticalPerson` |
| Conjugation variant | One verb paired with one grammatical person — the unit of progress | Conjugation | *step*, *cell* | `ConjugationVariant` |
| Conjugation table | A verb's full set of forms, asked as one exercise | Conjugation | *question* | `ConjugationTable` |
| Conjugated form | What a verb becomes for a given person | Conjugation | *answer*, *inflection* | `formsFor(person)` |
| Ending | The part of a conjugated form that differs from the verb's shared stem | Conjugation | *termination* | `ConjugationSplit.endings` |
| Stem | The longest prefix every usable form of a verb shares | Conjugation | *root*, *base* | `ConjugationSplit.stem` |
| Variant mastery | A conjugation variant answered correctly enough times in a row | Conjugation | *learned* | `MASTERY_STREAK` |
| Conjugation course | A chosen set of verbs practised together, with its own progress | Conjugation | *course* unqualified | `ConjugationCourse` |
| Catalogue | A body of shipped data seeded into the database from an asset | Catalogue | *database*, *source* | `SeedCatalogsUseCase` |
| Seeding | Writing a catalogue's asset into the database | Catalogue | *sync*, *import*, *download* | `seedFromAsset()` |
| Fingerprint | A cheap identity for an asset, used to skip unchanged seeding | Catalogue | *hash* | `CatalogSeedStore` |

## Entities

Objects with identity that persists across changes.

| Entity | Identity | Context | Behaviour it carries |
| --- | --- | --- | --- |
| Word | `VocabularyId` | Vocabulary | `addToStudySet`, `removeFromStudySet`, `edited`, `isPhrase` |
| Preset | `PresetId` | Vocabulary | `wordCount`, `studySetState` |
| Session | `SessionId` | Training | `answer`, `currentStep`, `isComplete`, the tallies |
| Program | `ProgramId` | Program | — (configuration is read whole) |
| Course | `CourseId` | Course | `completedCount`, `currentLesson`, `isComplete` |
| Lesson | `LessonId` | Course | — |
| Conjugation course | `ConjugationCourse.id` | Conjugation | — |
| Verb | infinitive | Conjugation | `formsFor`, `split`, `isComplete` |

## Value Objects

Defined wholly by their values, with no identity.

**Vocabulary** — `VocabularyId`, `PresetId`, `CefrLevel`, `LocalizedText`,
`PresetCategory`, `PresetStudySetState`

**Training** — `SessionId`, `TrainingType`, `StepOutcome`, `Step` (`Question` | `Board`)

**Scheduling** — `RecallQuality`, `ReviewState`, `ReviewSettings`, `StudyTimePolicy`

**Program** — `ProgramId`, `ProgressMetric`, `ProgressMetricType`, `ProgressWeights`,
`ScopeOrdering`, `ScopeSourceType`, `ActivityType`, `TargetType`, `LearningStrategy`,
`AdaptationTrigger`, `AdaptationAction`

**Course** — `CourseId`, `LessonId`, `LessonSummary`, `LessonExercise` and its item types

**Conjugation** — `GrammaticalPerson`, `ConjugationVariant`, `ConjugationSplit`,
`ConjugationAnswerMode`, `ConjugationStep`, `ConjugationVariantProgress`

`ConjugationVariant` is a value object even though progress is tracked per variant: the
pairing has no identity of its own, only the verb and the person.

## Aggregates and Aggregate Roots

Two aggregates enforce their invariants in code. The rest are consistency boundaries the
code respects without an owning root, and the reason is given for each — a root that only
reads its parts back out is ceremony.

| Aggregate Root | Contains | Invariant | Enforced? |
| --- | --- | --- | --- |
| **Session** | its ordered steps and their outcomes | a session has at least one step; steps are numbered from zero in order; a step is answered exactly once; the expected answer is the session's, not the caller's | **Yes** — `require`, `StepAlreadyAnswered`, `NoSuchStep` |
| **Word** | its own text, translation, transcription and study-set flag | a word always has text; study-set transitions go through the entity | **Yes** — `require` |
| Preset | its words and membership overrides | a hand-made membership survives a catalogue re-seed | In the seeder, with tests that fail if the carve-out is reverted. Owning a thousand words to protect it would cost more than it saves |
| Program | its config, days, milestones and rewards | a day's queue matches the config that produced it | By construction: the day is generated from the config and stored with it |
| Course | its lessons, their words, audio and exercises | lesson progress belongs to exactly one course | By the schema — progress is keyed by lesson |
| Conjugation course | its chosen verbs and their per-variant progress | progress is scoped to one course, so two courses over one verb do not share it | By the schema — progress is keyed by course |
| Verb | its forms per person | a form is never invented; absent means absent | By construction — forms are read from the asset, never derived |

## Domain Services

Operations that belong to no single entity.

| Service | Responsibility | Code |
| --- | --- | --- |
| Review scheduler | Turns an outcome into the next due date | `ReviewState.next()` |
| Recall grading | Turns a step outcome into a recall quality, or nothing for a word only shown | `StepOutcome.recallQuality()` |
| Study time policy | Decides how much of a gap between answers was studying | `StudyTimePolicy.creditedSeconds()` |
| Scope resolver | Turns a program's declared sources into words | `ResolveProgramScopeUseCase` |
| Scope ordering | Puts a program's words in the order it asks for | `ScopeOrdering.applyTo()` |
| Queue resolver | Finds the next training a day can actually run | `NextProgramTrainingUseCase` |
| Membership state | Decides whether a preset is fully, partly or not at all in the study set | `PresetStudySetState.of()` |
| Answer normaliser | Decides whether a written or spoken answer matches | `AnswerNormalizer` |
| Conjugation splitter | Derives stem and endings from a verb's own forms | `VerbConjugation.split()` |
| Voice choice | The voice the learner will hear: theirs, or the first on offer | `List<SpeechVoice>.chosen()` |

## Ports

Everything the domain needs from the outside, declared in `boundary` and implemented in
`data` (persistence) or `android` (device and network).

**Repositories** — `VocabularyRepository`, `VocabularyPresetRepository`,
`CourseRepository`, `ConjugationRepository`, `ProgramRepository`,
`TrainingHistoryRepository`, `ReviewScheduleRepository`, `StudyRecordRepository`,
`SettingsRepository`, `SessionStore`

**External services** — `Translator`, `SentenceGenerator`, `ImageProvider`,
`SpeechSynthesizer`, `SpeechRecognizerService`, `AudioPlayer`, `LessonAudioPlayer`,
`LessonAudioLibrary`

**Platform** — `AppVersionProvider`, `CatalogSeedGate`

## Domain Events

The system has **no explicit event type**, deliberately. Every candidate has exactly one
consumer, invoked synchronously in the same transaction, so an event bus would add
indirection and buy nothing. What would be events are recorded facts or state transitions:

| Event in the domain | How it is represented |
| --- | --- |
| A step was answered | `RecordAnswerUseCase` |
| A word became due | derived from `dueAtEpochDay` |
| A day was completed | `ProgramDay.isComplete` |
| A learner enrolled or left | `EnrolmentStatus` |
| A milestone was reached | a row in `program_milestone` |
| A catalogue changed | fingerprint mismatch |

Introducing an event type is a domain change and requires updating this document.

## Business Operations

| Operation | Meaning | Entry point |
| --- | --- | --- |
| Star a word | Add it to the study set, creating it if it does not exist | `ToggleWordInStudySetUseCase`, `ToggleVerbInStudySetUseCase` |
| Star a preset | Put every word of a preset into the study set, or take them out | `SetPresetInStudySetUseCase` |
| Start a session | Draw words and open a session over them | `Start*SessionUseCase` |
| Submit an answer | Mark one step against the session's expected answer | `Submit*UseCase` |
| Record an answer | Store the result, advance the review schedule, credit the study day | `RecordAnswerUseCase` |
| Advance the day | Mark the current training done and find the next runnable one | `NextProgramTrainingUseCase` |
| Enrol / leave | Begin or abandon a program | `EnrolInProgramUseCase`, `LeaveProgramUseCase` |
| Create a conjugation course | Fix a set of verbs as a course | `CreateConjugationCourseUseCase` |
| Restore the verbs | Re-seed the verb catalogue from its asset | `RestoreConjugationVerbsUseCase` |
| Seed the catalogues | Write shipped assets into the database | `SeedCatalogsUseCase` |

## Domain States

| Concept | States |
| --- | --- |
| Step outcome | `CORRECT`, `INCORRECT`, `SKIPPED`, `SEEN` |
| Step shape | `Question` (one word, one expected answer), `Board` (several words, paired) |
| Preset membership | `NONE`, `SOME`, `ALL` |
| Enrolment | `ACTIVE`, `COMPLETED`, `ABANDONED` |
| Catalogue step | `Pending`, `InProgress`, `Complete`, `Failed` |
| Answer (UI) | `Unanswered`, `Correct`, `Incorrect`, `Skipped` |
| Conjugation answer mode | `FULL_FORM`, `ENDING` |
| Verb completeness | complete, partial, unusable |
| Scope ordering | `AS_LISTED`, `FREQUENCY`, `DIFFICULTY`, `ALPHABETICAL`, `RANDOM` |

**Verb completeness** is domain-significant: the source records verbs with every person,
verbs with only some (`boleć` has only the third persons), and verbs with none. Only
usable forms become questions.

**Frequency is the catalogue own numbering.** The shipped vocabulary is ordered by how
common a word is — the Top 100 preset is ids 1..100 — so `FREQUENCY` orders by id and
`DIFFICULTY` orders by CEFR level.

## Relationships Between Concepts

```
Preset --< Word >-- Study set
                      |
                      +--> Session --< Step --> Outcome --> Training result
                      |                                          |
                      |                                          v
                      |                                    Review schedule
                      |                                          |
                      v                                          v
                  Program --> Program day --> Queue --> Training

Course --< Lesson --< Exercise
Verb --< Conjugated form        Conjugation course --< Verb
   |                                     |
   +--> Conjugation variant <------------+ --> Variant progress
```

A **word** is the unit of vocabulary; a **verb** is not a word — verbs live in their own
catalogue and only become words if the learner stars one.

## Context-Specific Terminology

These words carry different meanings in different contexts and must not be unified.

| Word | In one context | In another |
| --- | --- | --- |
| **Course** | Course — a sequence of authored lessons (Krok po kroku) | Conjugation course — a chosen set of verbs |
| **Mastery** | Scheduling: a word whose review interval passed the threshold (21 days by default) | Conjugation: a variant answered correctly twice in a row |
| **Progress** | Program: weighted metrics combined into a figure | Conjugation: variants mastered out of total |
| | | Course: lessons completed |
| **Step** | Training: one question in a session | Conjugation: one person row inside a table |
| | | Catalogue: one catalogue being seeded |
| **Difficulty** | Vocabulary: the CEFR level of a word | Program: an ordering, which reads the CEFR level |
| **Variant** | Conjugation: a verb-and-person pairing | UI: a styling option (`AnswerChipVariant`) — not domain |
| **Word** | Vocabulary: an entity in the study set | Word search: a string hidden in a grid |
| **Session** | Training: one run of a training | Program: `ProgramSession`, the words for the next activity |

## Known gaps

Recorded rather than hidden. Each is a place where the code does not yet match the model
this document describes.

- **Conjugation has no model package.** `VerbConjugation`, `ConjugationVariant`,
  `ConjugationSplit` and `MASTERY_STREAK` still live in `interactors.conjugation`. They
  are well modelled; they are simply in the application layer.
- **Catalogue has no model package.** Seeding is orchestration over ports, with the only
  rule — the fingerprint comparison — inside the repositories.
- **`ProgramConfig` is a stored format**, `@Serializable`, in `interactors`. Its enums are
  in the model because the rules switch on them; the structures around them are not
  duplicated because no rule needs them.
- **The preset list carries word ids it no longer displays.** Since counts and membership
  state come from SQL, the ids are dead weight; removing them needs a read-model type so
  that `vocabularyIds` is not silently empty for listed presets.

## Deprecated Terms

| Term | Status | Use instead |
| --- | --- | --- |
| Favourite | Two names for one concept | Study set — the term is gone from the codebase |
| Sync / `syncFromSource` | Renamed | Seeding / `seedFromAsset` |
| `ConjugationQuestion` | Renamed | `ConjugationTable` |
| Per-training outcome enums | Removed, nine of them | `StepOutcome` |
| `TrainingResultOutcomeBoundary` | Removed — it duplicated `StepOutcome` | `StepOutcome`, which carries `SEEN` |
| `TrainingRequirements` | Removed — a domain policy that lived in the UI module | `TrainingType.minimumWords` |
| `TRAINING_TYPE_*` constants | Removed, eleven of them | `TrainingType` |
| `TrainingIds` | Derived from the model rather than declaring a second encoding | `TrainingType.id` |
| `VocabularyItemBoundary` | Removed — an anemic twin of the word | `Word` |
| `PresetWord` | Renamed — it was never preset-specific | `Word` |
| `ProgramQueue` | Moved out of the presentation module | `NextProgramTrainingUseCase` |
| `TrainingType.PASSAGE` | Removed — it stood in for both variants | `PASSAGE_WRITE`, `PASSAGE_BANK` |
| Selection (conjugation) | Removed | A conjugation course |
| Reset the course | Removed | Delete the course |

## Terminology Change History

| Date | Change | Reason |
| --- | --- | --- |
| 2026-08-19 | **`Session` became a real aggregate** | The language claimed a Session aggregate whose invariant was "every step records exactly one result". Nothing enforced it: `sessionId` was a `String`, and the submit request carried the expected answer in from the caller |
| 2026-08-19 | `Step` split into **`Question`** and **`Board`** | Memory Cards and Word Match ask for a board of words to be paired, not a question with one right answer |
| 2026-08-19 | **`Word` promoted to the model**, absorbing `PresetWord` and `VocabularyItemBoundary` | One concept had two representations: a modelled one and an anemic twin at the data edge |
| 2026-08-19 | **`TrainingType` introduced** | One concept had four string encodings and a minimum-words table in the UI module |
| 2026-08-19 | **Review scheduling moved into `model.scheduling`** | A Room repository owned SM-2, the review policy and the study-time rule |
| 2026-08-19 | **`SEEN` added to `StepOutcome`** | The model could not express a state the domain has |
| 2026-08-19 | *Mastery* split into **word mastery** and **variant mastery** | One term covered two unrelated rules |
| 2026-08-19 | **`PresetStudySetState` moved to the model** and counted rather than derived from ids | The answer should not depend on every id having loaded |
| 2026-08-19 | **Scope orderings implemented**; frequency is the catalogue numbering | `DIFFICULTY` had been doing frequency under the wrong name |
| 2026-08-19 | **Voice choice given one home** | Three places answered "which voice will the learner hear" differently |
| 2026-08-19 | *Favourite* renamed to **study set** throughout the code | The interface had always said study set |
| 2026-08-19 | Module **`domain` renamed to `application`** | It held use-case implementations, not a domain model |

## Enforcement

- A new domain concept is checked against this glossary before it is named.
- The same concept reuses the same term; a different concept takes a different term even
  if the English word is tempting.
- A terminology conflict is an architectural issue, recorded here, not worked around
  locally.
- A change of meaning updates the definition, the affected code, and the change history in
  the same commit.
- An established term is never renamed silently.
