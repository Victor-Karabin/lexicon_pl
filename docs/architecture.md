# Architecture

How the code is arranged, and the rules that keep it that way. The vocabulary it uses is
defined in [Universal DDD Language](domain/universal-ddd-language.md).

## Modules

```
model          the domain — pure Kotlin, no framework of any kind
   ^      ^
boundary   interactors        ports        use-case contracts
   ^   ^        ^
data android  application     adapters     use-case implementations
                  ^
             presentation
                  ^
                 app          composition root and navigation

common         clock, dispatchers, text folding — used by the layers above
shared         the iOS framework, re-exporting everything except android, presentation and app
baselineprofile  generates the app's baseline profile
```

| Module | Holds | Depends on |
| --- | --- | --- |
| `model` | Entities, value objects, aggregates, domain services, policies | nothing but the Kotlin standard library and immutable collections |
| `boundary` | Ports: repository interfaces and external-service interfaces | `model` |
| `interactors` | Use-case contracts and their request/response types | `model` |
| `application` | Use-case implementations — orchestration over ports | `model`, `boundary`, `interactors`, `common` |
| `data` | Room, DAOs, entities, asset seeding, mapping, the remote image, sentence, grammar and translation clients | `model`, `boundary`, `common` |
| `android` | Text to speech, speech recognition, audio, Google Cloud clients | `model`, `boundary`, `common` |
| `presentation` | Compose screens, ViewModels, UI state | `model`, `boundary`, `interactors`, `common` |
| `app` | Koin modules, navigation, the single Activity | `common`, `boundary`, `interactors`, `application`, `data`, `android`, `presentation` (and `model` through them) |
| `common` | Clock, dispatchers, text folding, Polish transcription | nothing |
| `shared` | The framework the iOS app links against | `model`, `boundary`, `common`, `interactors`, `application`, `data` |
| `baselineprofile` | The macrobenchmark that generates the app's baseline profile | the `app` it targets |

## Rules

**The domain depends on nothing.** `model` has no Android, no Room, no Compose, no
serialization, no dependency injection. It is a plain Kotlin library, and a test of it
runs on the JVM in milliseconds.

**Ports are declared where they are needed, implemented where the technology lives.**
`boundary` declares what the application needs from the outside — a repository, a speech
synthesiser, a translator — and `data` and `android` implement them. Nothing in the
domain or the application knows which database or which cloud is behind a port.

**Presentation talks to use cases, not to repositories.** It depends on `interactors` for
contracts and on `boundary` only for the speech, audio and device-language ports it drives
directly. It cannot see a repository, and it does not depend on `android`.

**Business rules live in the model.** If a rule can be stated without mentioning a screen,
a table or a network call, it belongs in `model` and is tested there. Orchestration —
loading, calling, saving — belongs in `application`.

**Mapping is explicit at every boundary.** A Room entity is not a domain object; a domain
object is not a UI state. Each edge maps deliberately, so a column rename cannot ripple
into a screen.

## Where things are

| Looking for | Look in |
| --- | --- |
| What a word, session or review *is* | `model/<context>` |
| What the app can *do* | `interactors/<context>` — one interface per use case |
| How it does it | `application/<context>` |
| SQL, assets, seeding | `data/local`, `data/repository` |
| Speech, audio, cloud | `android` |
| Screens and their state | `presentation/<feature>` |
| Wiring | `app/di`, `application/di`, `data/di` |

## Persistence

Room, with a single `AppDatabase`. The schema takes the destructive fallback rather than
migrations: the project is pre-release, and a version bump drops and re-seeds. What the
seeders cannot refill — word statuses, hand-written words and presets, training history,
review schedules, the vocabulary course's place in its queue — goes with it, so a bump is a deliberate act.

Shipped data lives in `data/src/androidMain/assets` and is seeded on first launch and
whenever the app version changes. Each catalogue is fingerprinted, so an unchanged asset
costs a file read and no parse.

## Testing

Three layers, each testing what the one below it cannot.

**Unit tests**, on the JVM, in the module that owns the logic:

- **`model`** — pure domain tests: invariants, value-object rules, policies. No mocks.
- **`application`** — orchestration, with ports faked or mocked.
- **`data`** — mapping, seeding and the carve-outs that protect the learner's own data.
- **`presentation`** — ViewModel state transitions.
- **`app`** — a test that every Koin module's dependency graph resolves.

**Instrumented tests** in `data/src/androidInstrumentedTest`, against real SQLite. They
exist for the things a JVM test cannot see: how the text search matches, whether pages follow one
another without repeating, which known words the review queries return, and what happens
when a query binds more variables than SQLite takes — 999 on Android 8 to 11, which is
fewer than a large study set.

**End-to-end tests** in `app/src/androidTest`, driving the built app through Compose over
the real database and the shipped catalogues. They cover the journeys rather than the
screens: the app seeds and lands on its tabs, search narrows to the word asked for and a
word found that way can be marked to learn, every training is reachable, words sorted in
Review words are what a training then runs, and the vocabulary course launches a training
that offers its reset button.

    ./gradlew connectedDebugAndroidTest    # both, on a connected device or emulator

They share one database and one install, so a test that needs a particular state puts the
app into it rather than assuming the test before it did.

Espresso is pinned to 3.7: earlier versions call `InputManager.getInstance`, which newer
Android no longer has, and every Compose test fails at the first `waitForIdle`.

## Conventions

- **No comments in code.** Explanation goes in the commit message and the pull request,
  where it is read with the change that motivated it and cannot drift from it. Where
  something fails silently, it gets a log rather than a comment.
- **A file is named after its main declaration.** ktlint's `filename` rule enforces that for a
  file holding a single top-level declaration; a file may hold several related ones, such as a
  sealed hierarchy or a screen with its private parts.
- **Immutable collections in UI state and domain types**, so Compose can skip and the
  model cannot be mutated from under a caller.
- **Named arguments** for anything with more than a couple of parameters.
