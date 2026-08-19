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
| Vocabulary | Words, presets, the study set, images and translations | `model.vocabulary` |
| Training | A single practice session and what it records | `model.training`, `interactors.<training>` |
| Scheduling | Review intervals, mastery, study days, streaks | `model.scheduling` |
| Program | The daily plan and its queue of trainings | `model.program`, `interactors.program` |
| Course | Fixed teaching material — lessons and exercises | `model.course`, `interactors.course` |
| Conjugation | Verbs, their forms, and courses over them | `interactors.conjugation` |
| Catalogue | Seeding shipped data into the database | `interactors.sync` |

The same English word means different things in different contexts. Those cases
are listed under [Context-Specific Terminology](#context-specific-terminology) and
must not be merged.

## Canonical Terms

| Term | Definition | Context | Synonyms | Avoid | Code Representation |
| --- | --- | --- | --- | --- | --- |
| Word | A Polish word or phrase with its English translation, IPA and optional picture | Vocabulary | vocabulary item | *entry*, *item*, *term* | `Word`, `VocabularyId`, `CefrLevel` |
| Study set | The words the learner has chosen to practise | Vocabulary | — | — | `isInStudySet`, `studySetWordIds()` |
| Preset | A named, shipped or hand-made grouping of words by topic | Vocabulary | word list | *category* (that is the grouping above presets) | `VocabularyPreset`, `PresetId` in `model.vocabulary` |
| Preset category | A grouping of presets | Vocabulary | — | *topic* | `PresetCategory` |
| Membership | Whether a word belongs to a preset, including a hand-made override | Vocabulary | — | *link*, *relation* | `PresetMembership` |
| Training | A kind of exercise — dictation, crossword, word search | Training | exercise type | *game*, *test* | `TrainingType`; `trainingCatalog` for its presentation |
| Session | One run of one training, start to result screen | Training | — | *training* (that is the kind), *round* | `Session`, `SessionId` |
| Step | One question inside a session | Training | question | *item*, *card* | `Step`; `*StepResponse` at the UI edge |
| Outcome | How one step was answered, or that it was only shown | Training | result | *score*, *status* | `StepOutcome` |
| Training result | One recorded answer, kept for scheduling and statistics | Training / Scheduling | — | *history entry* | `TrainingResultBoundary` |
| Recall quality | How well a word was remembered, as the scheduler grades it | Scheduling | — | *score* | `RecallQuality` |
| Review settings | The policy the scheduler applies: intervals, ease, mastery threshold | Scheduling | — | *config* | `ReviewSettings` |
| Study time policy | How much of the gap between two answers counts as studying | Scheduling | — | — | `StudyTimePolicy` |
| Minimum words | The smallest study set a training can build a session from | Training | — | *requirement* | `TrainingType.minimumWords` |
| Review | A later encounter with a word the learner has already met | Scheduling | — | *repetition* | `ReviewScheduleRepository`, `WordReviewEntity` |
| Due | A word whose review interval has elapsed | Scheduling | — | *pending*, *expired* | `dueAtEpochDay` |
| Word mastery | A word whose review interval has passed the settings' threshold | Scheduling | — | *learned*, *complete* | `ReviewState.isMastered(settings)` |
| Variant mastery | A conjugation variant answered correctly enough times in a row | Conjugation | — | *learned*, *complete* | `VariantProgress.isMastered`, `MASTERY_STREAK` |
| Study day | A calendar day on which the learner practised, with its totals | Scheduling | — | *session day* | `StudyDayBoundary` |
| Streak | Consecutive study days | Scheduling | — | — | `GetStudyStreakUseCase` |
| Program | A configuration that plans a learner's daily work | Program | — | *course*, *plan* | `Program`, `ProgramId`, `ProgramConfig` |
| Enrolment | The learner's participation in a program | Program | — | *subscription*, *membership* | `ProgramEnrolment`, `EnrolmentStatus` |
| Program day | One day's plan for a program, and how much of it is done | Program | — | *daily plan* | `ProgramDay` |
| Queue | The ordered trainings a program day asks for | Program | — | *playlist*, *schedule* | `QueuedTraining`, `ProgramQueue` |
| Activity | A unit of work in a program's plan, mapped to a training | Program | — | *task* | `PlannedActivity`, `ActivityType` |
| Program configuration | The stored, read-mostly description of a program: its goals, scope, plan and rules | Program | — | *settings* | `ProgramConfig` — a stored format, not a domain object |
| Word card | A word shown for learning rather than testing, before the day's trainings | Program | — | *flashcard* | `WordCard`, `GetWordCardsUseCase` |
| Course | Fixed teaching material — a sequence of lessons | Course | — | *program*, *class* | `Course`, `CourseId` in `model.course` |
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
| Review scheduler | Turns an outcome into the next due date | `ReviewState.next()` in `model.scheduling` |
| Scope resolver | Turns a program's declared sources into word ids | `ResolveProgramScopeUseCase`, `ScopeOrdering.applyTo` |
| Queue resolver | Finds the next training a day can actually run | `NextProgramTrainingUseCase` |
| Conjugation splitter | Derives stem and endings from a verb's own forms | `VerbConjugation.split()` |
| Answer normaliser | Decides whether a written or spoken answer matches | `AnswerNormalizer` |
| Sentence generator | Writes example sentences for a target word | `SentenceGenerator` |
| Speech synthesiser | Says a Polish word or sentence out loud | `SpeechSynthesizer` |
| Speech recogniser | Turns what the learner said into text | `SpeechRecognizerService` |

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
| Submit an answer | Mark one step against the session's expected answer | `Submit*UseCase` |
| Record an answer | Store the result, advance the review schedule, credit the study day | `RecordAnswerUseCase` |
| Advance the day | Mark the current training done and find the next runnable one | `AdvanceProgramDayUseCase`, `NextProgramTrainingUseCase` |
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
| **Mastery** | Scheduling: a word whose review interval passed the threshold (21 days by default) | Conjugation: a variant answered correctly twice in a row |
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
| `TrainingResultOutcomeBoundary` | Removed — it duplicated `StepOutcome` exactly | `StepOutcome`, which now carries `SEEN` |
| `TrainingRequirements` | Removed — a domain policy that lived in the UI module | `TrainingType.minimumWords` |
| `TRAINING_TYPE_*` constants | Removed, eleven of them | `TrainingType` |
| `VocabularyItemBoundary` | Removed — an anemic twin of the word, carrying `Long` and `String?` where the model had value objects | `Word` |
| `PresetWord` | Renamed — it was never preset-specific | `Word` |
| `ScopeSourceType.FAVOURITES`, `ProgramDraftProblem.NO_FAVOURITES` | Renamed — the deprecated term survived an earlier case-sensitive sweep | `STUDY_SET`, `EMPTY_STUDY_SET` |
| `ProgramQueue` | Renamed and moved out of the presentation module | `NextProgramTrainingUseCase` |

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
| 2026-08-19 | **`SEEN` added to `StepOutcome`; `TrainingResultOutcomeBoundary` removed** | The two enums became identical. The earlier reason for keeping the boundary copy — that only it carried `SEEN` — was the defect, not the justification: the model could not express a state the domain has |
| 2026-08-19 | **Review scheduling moved out of `data` into `model.scheduling`** | A Room repository owned SM-2, the review policy and the study-time rule. The application now invokes the scheduler through `RecordAnswerUseCase` |
| 2026-08-19 | **Session completed across all sixteen trainings**; `Step` split into `Question` and `Board` | Memory Cards and Word Match ask for a board of words to be paired, not a question with one right answer. Two shapes, stated as two, rather than making `expectedAnswer` nullable for the other fourteen |
| 2026-08-19 | *Passage* recorded as `PASSAGE_WRITE` / `PASSAGE_BANK`; `TrainingType.PASSAGE` removed | The session now carries which variant ran, so the stand-in that covered both produces nothing |
| 2026-08-19 | Program **enums moved to `model.program`**; `ProgramConfig` stays a stored format | The rules switch on these states, so they are domain vocabulary. The `@Serializable` structures around them are the persistence format and are named as such rather than duplicated into the model |
| 2026-08-19 | Module **`domain` renamed to `application`** | It held use-case implementations, not a domain model. `model` is the domain; `interactors` declares the use cases; `application` implements them |
| 2026-08-19 | **`Course` and `VocabularyPreset` moved to the model**, with their behaviour folded onto the types | `completedCount`, `currentLesson` and `wordCount` were extension functions beside the data classes. A course's `level` is a `CefrLevel` rather than a string |
| 2026-08-19 | **`Session` became a real aggregate** | The language had claimed a Session aggregate whose invariant was "every step records exactly one result". Nothing enforced it: `sessionId` was a `String`, and the submit request carried the expected answer in from the caller, so a client could rename the right answer. Nine trainings now draw it from the session |
| 2026-08-19 | **`ProgramQueue` moved out of the Compose module** and became `NextProgramTrainingUseCase` | The glossary already listed it as the *queue resolver* domain service, but it was a class in the UI module applying a policy that also lived there. `QueuedTraining` now carries a `TrainingType` rather than a string |
| 2026-08-19 | **Speech and audio ports moved from `android` to `boundary`** | The ports were declared inside the Android module, so every ViewModel that wanted to play a word depended on infrastructure. `presentation` no longer depends on `android` at all. The `java.util.Locale` parameter went with them: no caller ever passed anything but Polish |
| 2026-08-19 | **`Word` promoted to the model**, absorbing `PresetWord` and `VocabularyItemBoundary` | One concept had two representations: a modelled one in the application layer and an anemic twin at the data edge, with a mapper between them. `VocabularyId` and `CefrLevel` now reach the repository |
| 2026-08-19 | *Mastery* split into **word mastery** and **variant mastery** | One glossary term covered two unrelated rules — an interval threshold in Scheduling, a correct-answer streak in Conjugation. They were never the same measure |
| 2026-08-19 | **`TrainingType` introduced**; `TrainingIds`, eleven `TRAINING_TYPE_*` constants and `TrainingRequirements` folded into it | One concept had three string encodings — lowercase route ids, uppercase stored types, and a minimum-words table in the UI module |

## Enforcement

- A new domain concept is checked against this glossary before it is named.
- The same concept reuses the same term; a different concept takes a different
  term even if the English word is tempting.
- A terminology conflict is an architectural issue, recorded here, not worked
  around locally.
- A change of meaning updates the definition, the affected code, and the change
  history in the same commit.
- An established term is never renamed silently.
