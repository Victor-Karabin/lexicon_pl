# Lexicon

An Android app for learning Polish, built for one learner and shaped around how the
learning actually goes: you choose the words you want to know, practise them through a
range of trainings, and a review schedule decides what comes back and when.

## What it does

**A study set you choose.** The app ships with 2,563 Polish words graded A1 to C2, grouped
into 73 presets by topic and frequency. You star what you want to learn, by word or by
whole preset, and every training draws from that set. You can add words of your own; the
IPA transcription is worked out for you.

**Fifteen ways to practise.** Dictation, Dictation Puzzle, Puzzle, Image Test, Word Match,
True or False, Pronunciation Check, Read Aloud, Memory Cards, Crossword, Word Card, Read
and Write, Read and Choose, Word Search, and a Mix that shuffles them together. Some read
a word aloud and ask you to type it; some show a picture; some listen to you say it.

**Spaced repetition underneath.** Every answer is graded and feeds an SM-2 schedule, so a
word you know comes back in three weeks and a word you miss comes back tomorrow. Study
days, streaks and accuracy are recorded as you go.

**Longer structures on top.** A *program* plans a day's work over your study set and runs
its trainings as a queue. A *course* teaches a fixed body of material — the Krok po kroku
textbook, with its own audio and exercises. A *conjugation course* drills the forms of
verbs you pick from a catalogue of 4,545.

**Written and spoken by machine where it helps.** Sentences for the reading trainings are
written by OpenAI around the word being practised. Speech is synthesised through Google
Cloud Text-to-Speech, with the device's own engine as a fallback, and pronunciation is
judged by Google Cloud Speech-to-Text. Pictures come from Pexels, Pixabay, Unsplash and
Openverse.

## Tech stack

| | |
| --- | --- |
| Language | Kotlin 2.1, Kotlin Multiplatform for the shared modules |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Persistence | Room 2.7 over SQLite, DataStore for preferences |
| Injection | Koin 4 |
| Networking | Retrofit 2 and OkHttp 4, kotlinx.serialization |
| Images | Coil |
| Speech | Google Cloud Text-to-Speech and Speech-to-Text, Android TTS and SpeechRecognizer as fallbacks |
| Language model | OpenAI, for example sentences and translations |
| Testing | JUnit 4, MockK, Turbine |
| Static analysis | ktlint, detekt, Android Lint |
| Build | Gradle 8.12 with version catalogs, AGP 8.7 |
| Targets | Android 8.0 and up (minSdk 26, target 35); an iOS app links the shared framework |

## Layout

Ten Gradle modules, layered so the domain depends on nothing:

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

[docs/architecture.md](docs/architecture.md) explains the rules and where to find things.

## Building

```
./gradlew build          # everything, with tests and static analysis
./gradlew :app:installDebug
```

Requires JDK 17. The app builds and runs without any keys — the features that need them
degrade rather than fail — but to have all of them, put these in `local.properties`,
which is not tracked:

```
openai.apiKey=...
google.ttsApiKey=...
deepl.apiKey=...
pexels.apiKey=...
pixabay.apiKey=...
unsplash.accessKey=...
openverse.clientId=...
openverse.clientSecret=...
```

Firebase is optional: drop in `google-services.json` and Crashlytics is wired up, leave it
out and the build skips those plugins.

## Data

Shipped catalogues live in `data/src/androidMain/assets` and are seeded into the database
on first launch and after an app update. Each is fingerprinted, so an unchanged asset
costs a file read and no parse.

The schema takes Room's destructive fallback rather than migrations — the project is
pre-release, so a version bump drops the database and re-seeds it, taking the study set and
history with it. That makes a bump a deliberate act.

## Documentation

| | |
| --- | --- |
| [Architecture](docs/architecture.md) | Modules, layering rules, where things live, conventions |
| [Universal DDD Language](docs/domain/universal-ddd-language.md) | The vocabulary: contexts, entities, aggregates, services |
| [Vocabulary and presets](docs/vocabulary-presets.md) | How the catalogue, presets and the study set work |
| [Course](docs/course.md) | The Krok po kroku course: lessons, audio, exercises |

## Status

Pre-release, version 0.1.0. 504 Kotlin files, 76 test files, no instrumentation tests.
