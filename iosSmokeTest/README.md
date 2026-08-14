# iOS smoke test

A throwaway app whose only job is to prove the shared Kotlin code actually runs on
iOS, rather than merely compiling for it. It checks the two halves of what the KMP
migration moved:

- a use case resolved from Koin and executed (shared business logic), and
- a real query against a Room database opened through the bundled SQLite driver on
  an iOS filesystem path (shared persistence).

This is not the real iOS app, and it is deliberately not part of the Gradle build.

## Running it

Build the framework first — the project references
`../shared/build/XCFrameworks/release/Shared.xcframework`, which does not exist
until this runs:

```bash
./gradlew :shared:assembleSharedReleaseXCFramework
```

Then open `LexiconSmokeTest.xcodeproj` in Xcode and run it on a simulator, or from
the command line:

```bash
xcodebuild -project iosSmokeTest/LexiconSmokeTest.xcodeproj -scheme LexiconSmokeTest -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.0.1' -derivedDataPath iosSmokeTest/build build
```

The app prints one `OK`/`FAIL` line per check on screen.

## Two things worth knowing

**`-lsqlite3` is required.** Room's `sqlite-framework` cinterop references the
system SQLite, so any iOS host linking this framework needs `-lsqlite3` in
`OTHER_LDFLAGS`. Without it the link fails on missing `sqlite3_*` symbols.

**Use the release XCFramework.** The debug variant fails to link with unresolved
`sqlite3_win32_*` symbols — Kotlin/Native's cinterop *cache* emits a wrapper for
every function `sqlite3.h` declares, including the Windows-only ones that Apple's
libsqlite3 does not export. The release build is produced without those caches and
links cleanly.

## What it does not cover

The JSON catalogues are Android assets and are not in this app's bundle, so nothing
here seeds the database — the word count is expected to be 0. Bundling the
catalogues for iOS is part of building a real iOS app, not of this smoke test.
