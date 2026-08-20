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
```

| Module | Holds | Depends on |
| --- | --- | --- |
| `model` | Entities, value objects, aggregates, domain services, policies | nothing but the Kotlin standard library and immutable collections |
| `boundary` | Ports: repository interfaces and external-service interfaces | `model` |
| `interactors` | Use-case contracts and their request/response types | `model` |
| `application` | Use-case implementations — orchestration over ports | `model`, `boundary`, `interactors`, `common` |
| `data` | Room, DAOs, entities, asset seeding, mapping | `model`, `boundary`, `common` |
| `android` | Text to speech, speech recognition, audio, Google Cloud clients | `model`, `boundary`, `common` |
| `presentation` | Compose screens, ViewModels, UI state | `model`, `boundary`, `interactors`, `common` |
| `app` | Koin modules, navigation, the single Activity | everything |
| `common` | Clock, dispatchers, text folding, Polish transcription | nothing |
| `shared` | The framework the iOS app links against | everything |

## Rules

**The domain depends on nothing.** `model` has no Android, no Room, no Compose, no
serialization, no dependency injection. It is a plain Kotlin library, and a test of it
runs on the JVM in milliseconds.

**Ports are declared where they are needed, implemented where the technology lives.**
`boundary` declares what the application needs from the outside — a repository, a speech
synthesiser, a translator — and `data` and `android` implement them. Nothing in the
domain or the application knows which database or which cloud is behind a port.

**Presentation talks to use cases, not to repositories.** It depends on `interactors` for
contracts and on `boundary` only for the speech and audio ports it drives directly. It
cannot see a repository, and it does not depend on `android`.

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
| SQL, assets, migrations | `data/local`, `data/repository` |
| Speech, audio, cloud | `android` |
| Screens and their state | `presentation/<feature>` |
| Wiring | `app/di`, `application/di`, `data/di` |

## Persistence

Room, with a single `AppDatabase`. The schema takes the destructive fallback rather than
migrations: the project is pre-release, and a version bump drops and re-seeds. What the
seeders cannot refill — the study set, hand-written words and presets, training history,
review schedules, program state — goes with it, so a bump is a deliberate act.

Shipped data lives in `data/src/androidMain/assets` and is seeded on first launch and
whenever the app version changes. Each catalogue is fingerprinted, so an unchanged asset
costs a file read and no parse.

## Testing

Unit tests only; there is no instrumentation or UI test infrastructure.

- **`model`** — pure domain tests: invariants, value-object rules, policies. No mocks.
- **`application`** — orchestration, with ports faked or mocked.
- **`data`** — mapping, seeding and the carve-outs that protect the learner's own data.
- **`presentation`** — ViewModel state transitions.
- **`app`** — a test that every Koin module's dependency graph resolves.

## Conventions

- **No comments in code.** Explanation goes in the commit message and the pull request,
  where it is read with the change that motivated it and cannot drift from it. Where
  something fails silently, it gets a log rather than a comment.
- **One declaration per file**, named after it. ktlint enforces this.
- **Immutable collections in UI state and domain types**, so Compose can skip and the
  model cannot be mutated from under a caller.
- **Named arguments** for anything with more than a couple of parameters.
