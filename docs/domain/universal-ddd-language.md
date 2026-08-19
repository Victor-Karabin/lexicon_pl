# Universal DDD Language

The authoritative vocabulary for Lexicon. If a term appears here, it means what
this document says it means, everywhere — in class names, use cases, tests,
documentation and user-facing copy.

This document is derived from the code as it stands, not from an idealised model.
Where the code and this document disagree, that is a defect in one of them; the
[Terminology Change History](#terminology-change-history) records which was fixed.

## Domain Overview

Lexicon teaches Polish to an English speaker. The learner keeps a **study set** of
words they want to know, practises them through **trainings**, and their answers
feed a **review schedule** that decides what comes back and when. Longer-running
structures sit above that: a **program** plans a day's work, and **courses** teach
a fixed body of material.

Everything the learner practises comes from one of three sources: the shipped
**vocabulary catalogue**, the **course** content, or the **verb catalogue**.

## Bounded Contexts

| Context | Owns | Package |
| --- | --- | --- |
| Vocabulary | Words, presets, the study set, images and translations | `interactors.presets` |
| Training | A single practice session and what it records | `interactors.<training>` |
| Scheduling | Review intervals, mastery, study days, streaks | `boundary.LearningRecordBoundary` |
| Program | The daily plan and its queue of trainings | `interactors.program` |
| Course | Fixed teaching material — lessons and exercises | `interactors.course` |
| Conjugation | Verbs, their forms, and courses over them | `interactors.conjugation` |
| Catalogue | Seeding shipped data into the database | `interactors.sync` |

The same English word means different things in different contexts. Those cases
are listed under [Context-Specific Terminology](#context-specific-terminology) and
must not be merged.

## Canonical Terms

| Term | Definition | Context | Synonyms | Avoid | Code Representation |
| --- | --- | --- | --- | --- | --- |
| Word | A Polish word or phrase with its English translation, IPA and optional picture | Vocabulary | vocabulary item | *entry*, *item*, *term* | `VocabularyItemBoundary`, `PresetWord`, `VocabularyId` |
| Study set | The words the learner has chosen to practise | Vocabulary | — | — | `isInStudySet`, `studySetWordIds()` |
| Preset | A named, shipped or hand-made grouping of words by topic | Vocabulary | word list | *category* (that is the grouping above presets) | `VocabularyPreset`, `PresetId` |
| Preset category | A grouping of presets | Vocabulary | — | *topic* | `PresetCategory` |
| Membership | Whether a word belongs to a preset, including a hand-made override | Vocabulary | — | *link*, *relation* | `PresetMembership` |
| Training | A kind of exercise — dictation, crossword, word search | Training | exercise type | *game*, *test* | `TrainingIds`, `trainingCatalog` |
| Session | One run of one training, start to result screen | Training | — | *training* (that is the kind), *round* | `sessionId`, `Start*SessionUseCase` |
| Step | One question inside a session | Training | question | *item*, *card* | `stepIndex`, `*StepResponse` |
| Outcome | How one step was answered: correct, incorrect, skipped | Training | result | *score*, *status* | `StepOutcome`; `TrainingResultOutcomeBoundary` at the data edge |
| Training result | One recorded answer, kept for scheduling and statistics | Training / Scheduling | — | *history entry* | `TrainingResultBoundary` |
| Review | A later encounter with a word the learner has already met | Scheduling | — | *repetition* | `ReviewScheduleRepository`, `WordReviewEntity` |
| Due | A word whose review interval has elapsed | Scheduling | — | *pending*, *expired* | `dueAtEpochDay` |
| Mastery | The point at which a word or variant counts as known | Scheduling / Conjugation | — | *learned*, *complete* | `isMastered`, `MASTERY_STREAK` |
| Study day | A calendar day on which the learner practised, with its totals | Scheduling | — | *session day* | `StudyDayBoundary` |
| Streak | Consecutive study days | Scheduling | — | — | `GetStudyStreakUseCase` |
| Program | A configuration that plans a learner's daily work | Program | — | *course*, *plan* | `Program`, `ProgramId`, `ProgramConfig` |
| Enrolment | The learner's participation in a program | Program | — | *subscription*, *membership* | `ProgramEnrolment`, `EnrolmentStatus` |
| Program day | One day's plan for a program, and how much of it is done | Program | — | *daily plan* | `ProgramDay` |
| Queue | The ordered trainings a program day asks for | Program | — | *playlist*, *schedule* | `QueuedTraining`, `ProgramQueue` |
| Activity | A unit of work in a program's plan, mapped to a training | Program | — | *task* | `PlannedActivity`, `ActivityType` |
| Word card | A word shown for learning rather than testing, before the day's trainings | Program | — | *flashcard* | `WordCard`, `GetWordCardsUseCase` |
| Course | Fixed teaching material — a sequence of lessons | Course | — | *program*, *class* | `Course`, `CourseId` |
| Lesson | One unit of a course, with its words, audio and exercises | Course | — | *chapter*, *unit* | `Lesson`, `LessonId` |
| Exercise | A question inside a lesson, of a fixed authored shape | Course | — | *training*, *step* | `LessonExercise`, `GapFillItem` |
| Verb | A Polish infinitive with the forms the source records for it | Conjugation | — | *word* (a verb is not in the vocabulary catalogue) | `VerbConjugation` |
| Grammatical person | One of the six persons the source distinguishes | Conjugation | person | *pronoun*, *form* | `GrammaticalPerson` |
| Conjugation variant | One verb paired with one grammatical person — the unit of progress | Conjugation | — | *step*, *cell* | `ConjugationVariant` |
| Conjugation table | A verb's full set of forms, asked as one exercise | Conjugation | — | *question* | `ConjugationTable` |
| Conjugated form | What a verb becomes for a given person | Conjugation | form | *answer*, *inflection* | `formsFor(person)` |
| Ending | The part of a conjugated form that differs from the verb's shared stem | Conjugation | suffix | *termination* | `ConjugationSplit.endings` |
| Stem | The longest prefix every usable form of a verb shares | Conjugation | — | *root*, *base* | `ConjugationSplit.stem` |
| Conjugation course | A chosen set of verbs practised together, with its own progress | Conjugation | — | *course* unqualified, *program* | `ConjugationCourse` |
| Catalogue | A body of shipped data seeded into the database from an asset | Catalogue | — | *database*, *source* | `SeedCatalogsUseCase` |
| Seeding | Writing a catalogue's asset into the database | Catalogue | — | *sync*, *import*, *download* | `seedFromAsset()`, `SeedCatalogsUseCase` |
| Fingerprint | A cheap identity for an asset, used to skip unchanged seeding | Catalogue | — | *hash*, *checksum* | `CatalogSeedStore` |

## Entities

Objects with identity that persists across changes.

| Entity | Identity | Context |
| --- | --- | --- |
| Word | `VocabularyId` | Vocabulary |
| Preset | `PresetId` | Vocabulary |
| Program | `ProgramId` | Program |
| Course | `CourseId` | Course |
| Lesson | `LessonId` | Course |
| Conjugation course | `ConjugationCourse.id` | Conjugation |
| Verb | infinitive | Conjugation |

## Value Objects

Defined wholly by their values, with no identity.

`LocalizedText`, `CefrLevel`, `GrammaticalPerson`, `ConjugationVariant`,
`ConjugationSplit`, `FillwordCell`, `FillwordDirection`, `TrainingReadiness`,
`ProgressWeights`, `ScopeSource`, `SyncOutcomeBoundary`, `AppSettings`.

`ConjugationVariant` is a value object even though progress is tracked per
variant: the pairing has no identity of its own, only the verb and the person.

## Aggregates and Aggregate Roots

| Aggregate Root | Contains | Invariant it protects |
| --- | --- | --- |
| Preset | its words and membership overrides | a hand-made membership survives a catalogue re-seed |
| Program | its config, days, milestones and rewards | a day's queue matches the config that produced it |
| Course | its lessons, their words, audio and exercises | lesson progress belongs to exactly one course |
| Conjugation course | its chosen verbs and their per-variant progress | progress is scoped to one course, so two courses over one verb do not share it |
| Verb | its forms per person | a form is never invented; absent means absent |
| Session | its steps and their outcomes | every step records exactly one result |

## Domain Services

Operations that belong to no single entity.

| Service | Responsibility | Code |
| --- | --- | --- |
| Review scheduler | Turns an outcome into the next due date | `LearningRecordBoundary` |
| Scope resolver | Turns a program's declared sources into word ids | `ResolveProgramScopeUseCase` |
| Queue resolver | Finds the next training a day can actually run | `ProgramQueue` |
| Conjugation splitter | Derives stem and endings from a verb's own forms | `VerbConjugation.split()` |
| Answer normaliser | Decides whether a written or spoken answer matches | `AnswerNormalizer` |
| Sentence generator | Writes example sentences for a target word | `SentenceGenerator` |

## Domain Events

The system has **no explicit event type**. What would be events are modelled as
recorded facts or state transitions:

| Event in the domain | How it is represented |
| --- | --- |
| A step was answered | `TrainingHistoryRepository.recordResult` |
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
| Start a session | Draw words and build the steps for one training | `Start*SessionUseCase` |
| Submit an answer | Mark one step, record it, and schedule the word | `Submit*UseCase` |
| Advance the day | Mark the current training done and find the next runnable one | `AdvanceProgramDayUseCase`, `ProgramQueue` |
| Enrol / leave | Begin or abandon a program | `EnrolInProgramUseCase`, `LeaveProgramUseCase` |
| Create a conjugation course | Fix a set of verbs as a course | `CreateConjugationCourseUseCase` |
| Restore the verbs | Re-seed the verb catalogue from its asset | `RestoreConjugationVerbsUseCase` |
| Seed the catalogues | Write shipped assets into the database | `SyncCatalogUseCase` |

## Domain States

| Concept | States |
| --- | --- |
| Step outcome | `CORRECT`, `INCORRECT`, `SKIPPED`, `SEEN` |
| Enrolment | `ACTIVE`, `COMPLETED`, `ABANDONED` |
| Catalogue step | `Pending`, `InProgress`, `Complete`, `Failed` |
| Answer | `Unanswered`, `Correct`, `Incorrect`, `Skipped` |
| Conjugation answer mode | `FULL_FORM`, `ENDING` |
| Verb completeness | complete, partial, unusable |

**Verb completeness** is domain-significant: the source records verbs with every
person, verbs with only some (`boleć` has only the third persons), and verbs with
none. Only usable forms become questions.

## Relationships Between Concepts

```
Preset ──< Word >── Study set
                      │
                      ├──> Session ──< Step ──> Outcome ──> Training result
                      │                                          │
                      │                                          v
                      │                                    Review schedule
                      │                                          │
                      v                                          v
                  Program ──> Program day ──> Queue ──> Training
                      
Course ──< Lesson ──< Exercise
Verb ──< Conjugated form        Conjugation course ──< Verb
   │                                     │
   └──> Conjugation variant <────────────┘ ──> Variant progress
```

A **word** is the unit of vocabulary; a **verb** is not a word — verbs live in
their own catalogue and only become words if the learner stars one.

## Context-Specific Terminology

These words carry different meanings in different contexts and must not be
unified.

| Word | In one context | In another |
| --- | --- | --- |
| **Course** | Course — a sequence of authored lessons (Krok po kroku) | Conjugation course — a chosen set of verbs |
| **Progress** | Program: weighted metrics combined into a figure | Conjugation: variants mastered out of total |
| | | Course: lessons completed |
| **Step** | Training: one question in a session | Conjugation: one person's row inside a question |
| | | Catalogue: one catalogue being seeded |
| **Variant** | Conjugation: a verb-and-person pairing | UI: a styling option (`AnswerChipVariant`) — not domain |
| **Word** | Vocabulary: an entity in the study set | Word search: a string hidden in a grid |
| **Session** | Training: one run of a training | Program: `ProgramSession`, the words for the next activity |

## Deprecated Terms

| Term | Status | Use instead |
| --- | --- | --- |
| Sync / `syncFromSource` | Renamed | Seeding / `seedFromAsset` |
| `ConjugationQuestion` | Renamed | `ConjugationTable` |
| Per-training outcome enums | Removed, nine of them | `StepOutcome` |
| Favourite | Two names for one concept: the interface said *study set*, the code said *favourite* | Study set everywhere; the term is gone from the codebase |
| Sync | Misleading — nothing is fetched from a network | Seeding, for catalogue loading |
| Selection (conjugation) | Removed | A conjugation course |
| Reset the course | Removed | Delete the course |

## Terminology Change History

| Date | Change | Reason |
| --- | --- | --- |
| 2026-08-19 | *Conjugation selection* → **conjugation course** | One implicit selection became several named courses |
| 2026-08-19 | *Question* redefined in Conjugation: now a whole verb, with **step** as its per-person row | A verb's table is asked at once, so the old unit needed a new name |
| 2026-08-19 | **Verb catalogue** introduced as a first-class catalogue | Verbs became deletable and restorable, so they had to be stored, not read from the asset each time |
| 2026-08-19 | *Sync* clarified as **seeding** | Nothing is fetched remotely; the word implied otherwise |
| 2026-08-19 | *Sync* → **seeding** throughout the catalogue context | Nothing is fetched over a network; the word said otherwise |
| 2026-08-19 | *ConjugationQuestion* → **ConjugationTable** | It was never one question — it is a verb's table, presenting one step per person |
| 2026-08-19 | Nine per-training outcome enums → one **`StepOutcome`** | One concept had nine declarations. `TrainingResultOutcomeBoundary` stays: boundary types are the data-edge contract, and it carries `SEEN`, which no training produces |
| 2026-08-18 | *Program title* stopped being stored data | It was never editable, so a stored copy could only go stale |
| 2026-08-19 | *Favourite* renamed to *study set* throughout the code | The interface had always said study set; the code name was the last holdout |

## Enforcement

- A new domain concept is checked against this glossary before it is named.
- The same concept reuses the same term; a different concept takes a different
  term even if the English word is tempting.
- A terminology conflict is an architectural issue, recorded here, not worked
  around locally.
- A change of meaning updates the definition, the affected code, and the change
  history in the same commit.
- An established term is never renamed silently.
