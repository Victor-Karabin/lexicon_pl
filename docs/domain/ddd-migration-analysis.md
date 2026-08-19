# DDD Migration Analysis

Analysis phase only. No code has been changed. Every claim below is a reading of
the code as it stands on `feature/ubiquitous-language`, with file references.

Naming follows [Universal DDD Language](universal-ddd-language.md). Where this
migration needs a concept the language does not yet name, the entry is proposed
here and lands in that document as part of the step that introduces it — the
language is the authority, not this file.

---

## 1. Current architecture analysis

Nine Gradle modules. Dependencies, as declared:

```
common      (no deps)          boundary (no deps)         interactors (no deps)
     \                              |                          /
      \                             |                         /
       `-------------------- domain -------------------------'
                                (impl of interactors, uses boundary)

data -> boundary, common          android -> common
presentation -> interactors, common, android
app -> everything                 shared (iOS) -> everything
```

| Module | Lines | What it actually holds |
| --- | ---: | --- |
| `boundary` | 511 | Repository interfaces + `*Boundary` DTOs (ports) |
| `interactors` | 1,945 | Use-case **interfaces** + request/response models |
| `domain` | 4,054 (+3,806 test) | Use-case **implementations** |
| `data` | 5,240 | Room, DAOs, entities, asset seeding |
| `presentation` | 20,147 | Compose, ViewModels |
| `android` | 1,414 | TTS, speech recognition, audio, cloud clients |
| `common` | 658 | Clock, dispatchers, **review scheduling**, transcription |
| `app` | 1,018 | Composition root |
| `shared` | 206 | iOS framework export |

**The module wiring is clean.** Verified by import scan: `data` imports nothing
from `interactors` (0 hits), `domain` imports nothing from `data` (0), and
`presentation` imports nothing from `boundary` or `data` (0). There are no
circular dependencies. Whatever else is wrong here, the layering discipline that
exists is real and should be preserved.

**The problem is a missing layer, not a broken one.** There is no domain model.
`interactors` holds anemic data classes that serve simultaneously as the
application-layer DTOs *and* as the de-facto domain model, consumed directly by
both `domain` and `presentation`. `domain` — despite its name — contains no model:
it is 91 files of procedural use-case implementations that read primitives out of
repositories, compute, and write primitives back.

The consequence is that business rules have settled wherever they were first
needed. Three examples, each verified below: the review scheduler runs inside a
Room repository; the queue resolver is a class in the Compose module; the concept
of a training session exists only as thirteen copies of the same mutable state in
thirteen ViewModels.

**Slicing is by training type, not by domain.** `interactors` and `domain` each
carry 15 parallel packages (`dictation`, `puzzle`, `crossword`, `imagetest`, …).
That is a feature slice. The bounded contexts the business actually has cut across
it: every one of those 15 packages participates in the same Training context and
feeds the same Scheduling context.

---

## 2. Identified bounded contexts

The seven contexts in the ubiquitous language are correct and are kept. What
changes is where each one's model lives — today five of the seven have no model at
all.

| Context | Business responsibility | Model today |
| --- | --- | --- |
| **Vocabulary** | Words, presets, the study set, membership | Anemic DTO (`VocabularyItemBoundary`) |
| **Training** | One session of one training and what it records | **None** — split across 15 packages and 13 ViewModels |
| **Scheduling** | Review intervals, mastery, study days, streaks | Good, but in `common`, invoked by infrastructure |
| **Program** | The daily plan and its queue | Config data classes; rules spread across 3 modules |
| **Course** | Authored lessons and exercises | Reasonable (`Course`, `Lesson` with id VOs) |
| **Conjugation** | Verbs, forms, conjugation courses | Reasonable; mastery rule in the wrong layer |
| **Catalogue** | Seeding shipped assets | Adequate; wrongly fused into `VocabularyRepository` |

Relationships (unchanged from the language document): Vocabulary is upstream of
everything — Training draws words from it, Scheduling records against word
identity, Program and Course select from it. Conjugation is **separate**: a verb
is not a word, and only becomes one if the learner stars it. That translation at
the boundary already exists and is correct.

Contexts are *not* being split along module lines. Vocabulary and Catalogue both
live partly in `data`; Training spans `interactors`, `domain` and `presentation`.
The migration moves models to match contexts, not modules to match contexts.

---

## 3–9. Proposed domain model, by context

Only the blocks with a business reason are listed. Blocks deliberately **not**
introduced are recorded in §9 with the reason.

### Training — the one genuinely missing aggregate

**Aggregate Root: `Session`** (identity: `SessionId`)
Contains: its ordered `Step`s, each with its expected answer and its recorded
`Outcome`.

*Business reason.* "One run of one training, start to result screen" is a real
thing the learner does, the language already defines it, and the code has no
representation of it. `sessionId` is a bare `String` in 24 signatures. The state
that makes a session a session — which step is current, what has been answered,
the running tallies, whether it is finished — is re-implemented in
`DictationViewModel`, `PuzzleViewModel`, `ImageTestViewModel`, `WordMatchViewModel`,
`TrueOrFalseViewModel`, `MemoryCardsViewModel`, `PassageViewModel`,
`PronunciationViewModel`, `DictationPuzzleViewModel`, `MixViewModel`,
`ExerciseViewModel` and two screens: thirteen copies of `correctCount`,
`incorrectCount`, `skippedCount`, `advanceToNextStep()`, `completeSession()`.

*Invariants it protects.*
1. Every step records exactly one outcome — the language already asserts this and
   nothing enforces it.
2. A step cannot be answered twice, and cannot be answered out of a completed
   session.
3. **The expected answer comes from the session, not from the caller.** Today
   `SubmitDictationAnswerRequest` carries `expectedText` and `vocabularyItemId`
   *in from the ViewModel* (`SubmitDictationAnswerUseCaseImpl.kt:21`), so the
   client tells the server what the right answer was. Nothing verifies the step
   belongs to the session. This is the strongest single argument for the aggregate.
4. A session is complete when every step has an outcome; tallies are derived, not
   accumulated by hand.

**Value Object: `TrainingType`**
*Business reason.* The kind of exercise is a closed set the domain reasons about —
minimum words, program activities, history filtering. It exists today in three
unrelated encodings: `TrainingIds.DICTATION = "dictation"` (presentation),
`TRAINING_TYPE_DICTATION = "DICTATION"` (nine private constants across `domain`),
and `trainingType: String` on the boundary and the Room row. Nothing maps between
the lowercase and uppercase sets.

Behaviour that belongs on it: `minimumWords` — currently
`TrainingRequirements.minimumWordsFor()` in the **presentation** module, passed
into `CheckTrainingReadinessUseCase` as a parameter by the UI.

**Value Objects: `SessionId`, `StepOutcome`**
`StepOutcome` already exists and is already unified; it moves to the model
unchanged. `SessionId` replaces the `String`.

**Domain Service: `AnswerNormalizer`** — already exists and is already a domain
service in substance (`domain/dictation/AnswerNormalizer.kt`). It answers "does
this written or spoken answer count as the expected one", spans several trainings,
and belongs to no single entity. It moves to the Training model package; the
misleading `dictation` home goes.

### Scheduling — the best-modelled part of the codebase, in the wrong place

`common/ReviewScheduling.kt` is already proper DDD: `ReviewState` is an immutable
value object with behaviour (`next()` is a pure function, plus `isLearned`,
`isMastered`), `ReviewSettings` is a policy value object, `RecallQuality` is a
value object with a rule (`isRecalled`). It has its own test.

Nothing needs modelling here. Two things need moving:

- The file moves from `common` (a utility module everything depends on) to the
  Scheduling model.
- **The domain must be the one to invoke it.** Today `ReviewState.next(...)` is
  called from `TrainingHistoryRepositoryImpl.recordResult()` — a Room class in the
  `data` module — which also default-constructs the policy (`ReviewSettings()`),
  decides what counts as a review (`existing != null`), and owns the study-time
  rule `MAX_GAP_BETWEEN_ANSWERS_SECONDS = 120`. Import scan confirms the domain
  never references `ReviewScheduling` at all; only `data` does.

**Value Object: `StudyTimePolicy`** — the 120-second rule. It decides how long a
gap between two answers still counts as studying. That is a business judgement
about what "studying" means, currently a private constant in a Room repository.

### Vocabulary

**Entity: `Word`** (identity: `VocabularyId`)
*Business reason.* A word has identity that survives editing, a lifecycle
(created → edited → starred → deleted → restored), and a catalogue re-seed must
preserve the learner's decisions about it. Today it is `VocabularyItemBoundary`, a
six-field DTO with `id: Long` and `cefr: String?`.

Behaviour that belongs on it: `star()` / `unstar()`, `delete()` / `restore()`,
`edit(text, translation, transcription)`. Modest, but real — and each is currently
a repository method that mutates a row.

**Value Objects: `VocabularyId`, `CefrLevel`**
`VocabularyId` already exists in `interactors.presets` and is used in the Program
and Course contexts — but the Vocabulary context itself passes `Long`. `CefrLevel`
is listed in the language as a value object and is a `String?` in the DTO and a
`String` parameter on `wordIdsForLevel(level: String)`.

**Aggregate Root: `Preset`** — already named in the language, with a real
invariant ("a hand-made membership survives a catalogue re-seed") that is
currently enforced by carve-out logic inside `VocabularySeeder`/`PresetDao`. Worth
modelling, but *after* the higher-value steps; the current carve-out works and is
tested.

### Program

**Aggregate Root: `Program`** with its config, days, milestones and rewards —
already named in the language.

The rules are all present but scattered:
- Scope resolution, session selection ("review first if anything is due, else
  learn fresh words") and weighted progress live in `domain/program/ProgramEngine.kt`,
  written procedurally over `Long` ids.
- **`ProgramQueue` — which the language lists as the "Queue resolver" domain
  service — is a class in `presentation/program/`.** It applies the readiness
  policy from `presentation.common.TrainingRequirements` and contains the rule
  that skips a queued training the day cannot actually run. Its test is in
  `presentation` too.

**Domain Services: `ScopeResolver`, `QueueResolver`** — both already exist in
substance, both named in the language, both in the wrong module.

**Value Object: `ProgressWeights`** — exists; the weighted-combination rule that
uses it is inlined in `GetProgramProgressUseCaseImpl`.

### Conjugation

Already the most coherently modelled context: `ConjugationVariant`,
`ConjugationSplit`, `GrammaticalPerson` are genuine value objects, and
`VerbConjugation.split()` is a genuine domain service (deriving stem and endings
from the verb's own forms).

One defect: **mastery is defined twice, differently, in two layers.**

| Context | Rule | Where |
| --- | --- | --- |
| Scheduling | `intervalDays >= 21` | `common/ReviewScheduling.kt` |
| Conjugation | `streak >= 2` | `interactors/conjugation/ConjugationModels.kt:76` |

The language lists **Mastery** as one canonical term spanning "Scheduling /
Conjugation". It is not one rule. Per the language document's own rule that
context-specific meanings must be listed and not merged, these need distinct
names — proposed: **word mastery** and **variant mastery** — and the glossary row
needs splitting.

### Course, Catalogue

Course is adequately modelled (`Course`, `Lesson`, `CourseId`, `LessonId`,
immutable lists, behaviour in extension functions). It needs `level: String` →
`CefrLevel` and its extensions folded onto the types. Low priority.

Catalogue's only real defect is structural: `seedFromAsset()` sits on
`VocabularyRepository` alongside 19 word-access methods, fusing two contexts into
one interface.

### 9. Domain Events — recommendation: introduce none

The language already records that the system has no explicit event type and models
domain facts as recorded state. Reviewing the candidates:

| Fact | Would an event help? |
| --- | --- |
| A step was answered | No — one synchronous consumer (scheduling). Explicit orchestration in the application service is simpler and testable. |
| A word became due | No — derived from `dueAtEpochDay` on read. |
| A milestone was reached | No — evaluated on demand, stamped in a row. |
| A catalogue changed | No — a fingerprint comparison. |

Every candidate has exactly one consumer, invoked synchronously, in the same
transaction. An event bus would add indirection and buy nothing. The one thing
that *looks* like it needs an event — "answering a step must update the review
schedule" — is better fixed by making the application service call the scheduler
explicitly, which is precisely what step 1 does. **No domain events, no event bus.**

---

## 10. Repository abstractions

Current problems, then the target shape.

`VocabularyRepository` (20 methods) is a data-access interface, not a domain
repository. It leaks the search index (`search(foldedQuery: String, …)` — the
caller must know the folding rule), returns anemic DTOs, deals in
`List<Long>` id-bags (`allWordIds()`, `wordIdsForLevel()`, `studySetWordIds()`),
and carries a Catalogue-context method (`seedFromAsset()`).

`ReviewScheduleRepository.countMastered(masteredIntervalDays: Long)` is told the
mastery threshold by its caller — the policy lives outside the model and is passed
in as a `Long`.

Target:

| Abstraction | Context | Shape |
| --- | --- | --- |
| `WordRepository` | Vocabulary | Domain-oriented: `find(VocabularyId)`, `studySet()`, `save(Word)`, `search(SearchQuery)`; returns `Word` |
| `PresetRepository` | Vocabulary | Loads and saves the `Preset` aggregate whole |
| `SessionRepository` | Training | Persists the `Session` aggregate |
| `TrainingStatistics` | Training/Scheduling | **Read model**, separate from the repository — accuracy, session counts, per-training history |
| `ReviewScheduleRepository` | Scheduling | Returns `ReviewState`; the threshold comes from the model, not the caller |
| `StudyRecordRepository` | Scheduling | Unchanged in shape |
| `CatalogueSeeder` | Catalogue | `seedFromAsset()` moves here, off `VocabularyRepository` |
| `SpeechSynthesizer`, `SpeechRecognizer`, `AudioPlayer` | — | **Ports move from `android` to `boundary`** |

Read models are deliberately kept off the repositories. Statistics queries
(`accuracyBetween`, `countSessionsOfTrainingBetween`) do not load aggregates and
should not pretend to.

---

## 11. Application services / use cases

The existing use-case interfaces in `interactors` are the right idea and mostly
keep their names. What changes is what they contain: today they *are* the business
rules; after migration they orchestrate.

`SubmitDictationAnswerUseCase`, taken as the representative case:

| | Today | Target |
| --- | --- | --- |
| Decide correct/incorrect | in the use case | `Session.answer(step, submitted)` |
| Know the expected answer | supplied by the ViewModel | held by the `Session` |
| Compute recall quality | in the Room repository | `StepOutcome` → `RecallQuality` in the model |
| Advance the schedule | in the Room repository | use case calls `ReviewState.next(...)` |
| Tally the session | in the ViewModel | derived from the `Session` |
| Persist | via `recordResult` | `sessionRepository.save(session)` |

The nine `Submit*AnswerUseCase` implementations collapse toward one orchestration
over the `Session` aggregate, with per-training differences remaining only where
they are genuine (crossword submits a whole grid; memory cards submit a pair).

`ProgramQueue` becomes an application service in `interactors`, delegating the
"can this training run" rule to `TrainingType.minimumWords`.

---

## 12. Infrastructure responsibilities

After migration, `data` and `android` do persistence and device I/O and nothing
else:

- `data`: Room entities, DAOs, asset parsing, fingerprinting, and **explicit
  mapping** between Room rows and domain objects. `TrainingHistoryRepositoryImpl`
  loses its scheduling logic, its `ReviewSettings()` construction, its
  new-vs-review decision and its 120-second constant.
- `android`: TTS, speech recognition, audio playback, Google Cloud clients —
  implementing ports declared in `boundary`.

Mapping stays explicit in both directions. Room entities are not reused as domain
models.

---

## 13. Dependency violations in the current architecture

Ordered by severity. Every one is verified against the code.

| # | Violation | Evidence |
| --- | --- | --- |
| V1 | **The review scheduler runs inside a Room repository.** Infrastructure owns SM-2, the review policy, the new-vs-review rule and the study-time rule | `data/repository/TrainingHistoryRepositoryImpl.kt:32-70`; import scan: only `data` references `ReviewScheduling` |
| V2 | **The `Session` aggregate does not exist.** Session state duplicated in 13 ViewModels; `sessionId: String` in 24 signatures | 13 files with `correctCount`/`advanceToNextStep` |
| V3 | **Session integrity is unenforceable.** The client supplies the expected answer and word id on submit | `SubmitDictationAnswerUseCaseImpl.kt:21-38` |
| V4 | **A documented domain service lives in the Compose module.** `ProgramQueue` — the language's "Queue resolver" | `presentation/program/ProgramQueue.kt` |
| V5 | **Training readiness policy lives in presentation** and is passed *into* the use case | `presentation/common/TrainingRequirements.kt`, `TrainingGateViewModel.check(minimumWords)` |
| V6 | **Training identity is stringly typed in three inconsistent encodings** | `TrainingIds` (lowercase), 9 × `TRAINING_TYPE_*` (uppercase), `trainingType: String` |
| V7 | **Mastery means two different things** with no record of the split | `ReviewScheduling.kt` vs `ConjugationModels.kt:76` |
| V8 | **Presentation depends on the `android` infrastructure module directly** — 18 imports across 14 ViewModels; the ports are declared in infrastructure | `import com.lexicon.android.speech.SpeechSynthesizer` etc. |
| V9 | **A domain policy is passed into a repository as a primitive** | `ReviewScheduleRepository.countMastered(masteredIntervalDays: Long)` |
| V10 | **`VocabularyRepository` is a data-access interface**: leaks the search index, deals in id-bags, fuses in the Catalogue context | `boundary/VocabularyRepository.kt` |
| V11 | **`common` is a shared kernel mixing domain with utilities** — review scheduling and Polish transcription next to `Clock` and `DispatcherProvider` | `common/` |
| V12 | **Primitive obsession** — `id: Long`, `cefr: String?`, outcome persisted as `outcome.name` | `VocabularyItemBoundary`, `TrainingResultEntity` |
| V13 | **The persistence format shapes the application model** — `@Serializable`/`@SerialName` on `ProgramConfig` | `interactors/program/ProgramConfig.kt:172` |
| V14 | `domain` depends on Koin | `domain/build.gradle.kts:29` — contained to `di/`, low severity |
| V15 | Deprecated term still in code, against the language | `ScopeSourceType.FAVOURITES`, `ProgramDraftProblem.NO_FAVOURITES` |

V15 note: these survived the study-set rename because that sweep was
case-sensitive and these are uppercase. They are folded into step 3, not fixed
separately.

---

## 14. Proposed target architecture

One new module, `model`: **pure Kotlin, zero dependencies** — no Koin, no
serialization, no coroutines, no Android plugin. This is the layer the project
does not have.

```
model            (pure Kotlin; entities, VOs, aggregates, domain services, policies)
   ^   ^     ^
   |   |     `-------------------------.
boundary   interactors                  \
(ports)    (application: use cases)      \
   ^   ^        ^                         \
   |   |        |                          \
 data android  domain (application impls)  presentation
```

Packages inside `model` are **bounded contexts**, not trainings:
`model/vocabulary`, `model/training`, `model/scheduling`, `model/program`,
`model/course`, `model/conjugation`, `model/catalogue`.

Why a new module rather than reshaping `domain` in place: adding a module is
purely additive and cannot break what exists, so each context can move on its own
commit with the build green throughout. Moving 91 implementation files out of
`domain` first would be a large rewrite before any value is delivered.

The naming debt this leaves — a module called `domain` that holds application
implementations, not the domain — is real, and is step 9: optional, mechanical,
and worth doing only once the model is populated.

---

## 15. Incremental migration steps, ordered by dependency and risk

Each step compiles, keeps behaviour, and ships with its tests before the next
begins.

| # | Step | Fixes | Risk | Why here |
| --- | --- | --- | --- | --- |
| 0 | Create the empty `model` module, wire it | — | None | Purely additive |
| 1 | Move `ReviewScheduling` to `model/scheduling`; add `ReviewPolicy` and `StudyTimePolicy`; make the application invoke the scheduler; `TrainingHistoryRepositoryImpl` becomes pure persistence | V1, V9, V11 | **Medium** | Highest value. Touches the write path of every training — but both sides have tests (`ReviewSchedulingTest`, `TrainingHistoryRepositoryImplTest`) |
| 2 | `TrainingType` value object; fold `minimumWords` onto it; delete the three string encodings | V5, V6 | Low | Mechanical, `TrainingRequirementsTest` moves with it |
| 3 | `Word` entity, `VocabularyId`, `CefrLevel`; reshape `VocabularyRepository`; split out `CatalogueSeeder`; fix `FAVOURITES` | V10, V12, V15 | Medium | Widely used; unblocks everything downstream |
| 4 | **`Session` aggregate** — pilot on Dictation only, then one training per commit | V2, V3 | **High** | The biggest change; deliberately last among the core steps, and staged 1 training at a time |
| 5 | Move `ProgramQueue` and `ProgramEngine` rules into `model/program`; `Program` aggregate | V4 | Medium | Depends on `TrainingType` from step 2 |
| 6 | Split mastery into word mastery / variant mastery; update the glossary | V7 | Low | Independent |
| 7 | Move speech/audio ports from `android` to `boundary` | V8 | Low | Independent, mechanical |
| 8 | `Preset` and `Course` aggregates | — | Low | Lowest value; both currently work |
| 9 | *Optional:* rename `domain` → `application`, or merge into `interactors` | naming | Low | Cosmetic; only once `model` is populated |

Steps 6 and 7 are independent of the rest and can be taken any time, including
first, if a lower-risk start is preferred.

---

## 16. Files and modules that must change

| Step | Principal files |
| --- | --- |
| 0 | `settings.gradle.kts`, new `model/build.gradle.kts` |
| 1 | `common/ReviewScheduling.kt` (moves), `data/repository/TrainingHistoryRepositoryImpl.kt`, `boundary/LearningRecordBoundary.kt` (also needs splitting — it declares two repositories and two DTOs), the 9 `Submit*UseCaseImpl` |
| 2 | `presentation/main/TrainingCatalog.kt`, `presentation/common/TrainingRequirements.kt`, 9 × `TRAINING_TYPE_*` constants, `boundary/TrainingResultBoundary.kt`, `data/local/TrainingResultEntity.kt` |
| 3 | `boundary/VocabularyRepository.kt`, `boundary/VocabularyItemBoundary.kt`, `data/repository/VocabularyRepositoryImpl.kt`, `data/local/VocabularySeeder.kt`, `interactors/program/ProgramConfig.kt`, `ProgramDraftProblem.kt` |
| 4 | `interactors/<15 packages>`, `domain/<15 packages>`, 13 ViewModels |
| 5 | `presentation/program/ProgramQueue.kt`, `domain/program/ProgramEngine.kt`, `domain/program/ProgramDayUseCasesImpl.kt` |
| 6 | `interactors/conjugation/ConjugationModels.kt`, `docs/domain/universal-ddd-language.md` |
| 7 | `android/speech/*`, `android/audio/*`, `android/recognition/*`, `boundary/`, 14 ViewModels |

The `app` module's Koin graph (`DomainModule.kt`, 281 lines) changes in every step.

---

## 17. Tests to add or change

The project has 76 test files and no UI test infrastructure (and none is being
added, per the standing constraint).

**Add, in `model` — pure JVM, no Android, no mocks:**
- `Session`: every step records exactly one outcome; a step cannot be answered
  twice; answering a completed session is rejected; tallies derive correctly;
  completion when and only when all steps are answered.
- `TrainingType`: minimum words per type — moved from `TrainingRequirementsTest`.
- `Word`: star/unstar, delete/restore transitions.
- `ReviewPolicy` / `StudyTimePolicy`: threshold behaviour at and either side of
  the boundary, including the 120-second gap clamp.
- Word mastery vs variant mastery: the two rules, asserted separately.

**Change:**
- `ReviewSchedulingTest` moves from `common` to `model` unchanged — it is already
  a pure domain test and is the safety net for step 1.
- `TrainingHistoryRepositoryImplTest` shrinks to persistence assertions as the
  scheduling logic leaves it. **The scheduling assertions must move, not vanish.**
- `ProgramQueueTest` moves from `presentation` to the application layer.
- The 13 ViewModel tests shrink as session logic leaves them; `DictationViewModelTest`
  is the regression net for the step-4 pilot and must keep passing untouched.

**Unchanged:** all `data` seeding tests, including the user-created survival tests.

---

## 18. Risks and unresolved architectural questions

**Risks.**
1. *Step 4 is genuinely large.* Thirteen ViewModels and fifteen training flows. It
   is staged one training per commit with Dictation as the pilot, and can be
   stopped after any commit with the build green and behaviour intact.
2. *Step 1 touches every training's write path.* A defect here corrupts review
   schedules silently — the learner would see wrong words at wrong times with no
   error. Mitigated by the existing tests on both sides, but this is the step to
   verify on a device.
3. *Nothing has been verified on a device since the conjugation work began.* That
   is a pre-existing gap this migration inherits and makes more dangerous.
4. *Schema.* Steps 1–3 change persisted shapes (`TrainingType`, `VocabularyId`,
   outcome storage). Under the standing in-development policy each takes a
   destructive version bump — but that wipes the learner's real data, which has
   already happened once in this project.

**Unresolved — insufficient verified data available.**

- **`ScopeOrdering.FREQUENCY` and `ALPHABETICAL` do nothing.** Both fall through to
  `included.toList()` in `ResolveProgramScopeUseCaseImpl`, and `DIFFICULTY` sorts
  by word id. Whether these are unimplemented or intentionally inert cannot be
  determined from the code. **Not silently "fixed" during migration** — it needs a
  decision.
- **`ScopeSourceType.LESSON` returns `emptyList()`.** Same question.
- **Whether `SEEN` should affect mastery.** It is excluded from scheduling today
  (`TrainingHistoryRepositoryImpl`) and no training produces it; behaviour is
  preserved as-is.
- **Whether the two mastery rules are meant to converge.** §6 assumes not, and
  names them apart. If they are meant to be one rule, that is a business decision,
  not a refactoring.
- **Whether `Preset` needs to be a real aggregate.** The re-seed invariant is
  currently enforced in the seeder and tested. Modelling it is defensible but its
  value is unproven — hence step 8, last.

---

## Migration status

All planned steps are complete.

| Step | State | Commit |
| --- | --- | --- |
| 0 — `model` module | Done | `815de11` |
| 1 — Scheduling into the model; application invokes it | Done | `815de11` |
| 2 — `TrainingType` | Done | `300fa6a` |
| 3 — `Word` entity, `VocabularyId`, `CefrLevel` | Done | `53f82c9` |
| 6 — Mastery split into word / variant | Done | `53f82c9` |
| 7 — Speech and audio ports out of `android` | Done | `161ce73` |
| 5 — Queue resolver out of the UI; program value objects | Done | `45799e5` |
| 4 — `Session` aggregate | Done | `4330055` |
| 8 — `Course` and `VocabularyPreset` to the model | Done | `e035cfb` |
| 9 — `domain` module renamed to `application` | Done | — |

Every step ends on a green `./gradlew build` — all modules, all unit tests,
ktlint, Android lint, and the iOS framework link.

**Verified on an emulator** (no physical device was touched). A fresh install
seeds 2,563 words and 73 presets, the app launches with no crash and no DI
failure, and a True or False session over a starred Top 100 wrote the write path
end to end:

| What | Observed | Expected |
| --- | --- | --- |
| Correct answer | `repetitions=1`, ease 2.6, due today+1 | SM-2 rewards a perfect recall |
| Wrong answer | `repetitions=0`, ease 1.96, `lapses=1`, due today+1 | back to the start of the ladder |
| `wasReview` | 0 on every row | all first exposures |
| `trainingType` | `true_or_false` | the `TrainingType.id` encoding |
| `study_day` | 10 answers, 7 correct, 10 new, 18s studied | tallies and the gap clamp |

That is the step-1 rewrite — scheduling moved out of the Room repository — doing
in production what its tests say it does.

### Final module shape

```
model          pure Kotlin: entities, value objects, aggregates, policies
   ^      ^         ^
boundary  interactors        (ports)   (use-case contracts)
   ^   ^        ^
data android  application    (adapters)          (use-case implementations)
                  ^
             presentation
```

`presentation` no longer depends on `android` at all. `model` depends on nothing
but the Kotlin stdlib and immutable collections.

### Where the analysis was wrong, and what implementing changed

- **§3 understated the Vocabulary model.** A properly modelled `PresetWord` — with
  `VocabularyId` and `CefrLevel` — already existed beside the anemic
  `VocabularyItemBoundary`. The defect was two representations of one concept, not
  an absent model.
- **A third and fourth training-id spelling existed** beyond the three counted in
  §13 V6: `"passage"` and `"word_card"`, declared inside their own use cases.
- **`SEEN` was the reason the two outcome enums could not merge**, not a reason to
  keep both.
- **Two boundary methods were dead** — `countSessionsOfTrainingBetween` and
  `resultsForWord` — and were removed rather than kept alive by a mapping that
  would misreport old rows.
- **No `ReviewPolicy` type was introduced.** `ReviewSettings` already was the
  policy object.
- **`Preset` was not made an owning aggregate.** Its invariant is enforced in the
  seeder and covered by tests that fail when the carve-out is reverted; owning its
  words would mean reading a thousand rows to answer questions that do not need
  them.
- **`Word Match` and `Memory Cards` are not on the `Session` aggregate.** Their
  step is a board of several words; `Step` models one word per step, and widening
  it for two trainings would weaken it for the other fourteen.
- **The `Session` invariant found a latent bug.** "A session must have at least one
  step" rejected the session a zero-word draw builds today — which is why such a
  training used to sit on Loading forever. The start use cases no longer open one.

### Still unresolved

- **`ScopeOrdering.FREQUENCY` and `ALPHABETICAL` do nothing** — both fall through to
  the unsorted list, and `DIFFICULTY` sorts by word id. Left as found: whether they
  are unimplemented or intentionally inert cannot be read from the code.
- **`ScopeSourceType.LESSON` returns an empty list.** Same question.
- **Passage Write and Passage Bank record as one training type**, because the submit
  request does not carry the variant. The session now holds it, so this can be
  closed without guessing — but it is a behaviour change, not a refactoring.
- **Whether the two mastery rules should converge** is a business decision.
- **`ProgramConfig` is still `@Serializable`** and lives in `interactors`. It is the
  stored configuration format rather than a domain concept; giving the model its own
  copy would mean mapping eleven sub-structures for no rule that needs it.
- **Nothing has been verified on a device.** Step 1 rewrote the write path of every
  training; a defect there corrupts review schedules silently.
