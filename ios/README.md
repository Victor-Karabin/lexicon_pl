# Lexicon for iOS

The SwiftUI app. It shares everything below the UI with Android — use cases,
repositories, the Room database, the review schedule — through the `Shared`
XCFramework the `:shared` Gradle module produces.

## Running it

Once, on a clean checkout:

```bash
ios/bootstrap.sh
```

That builds the `Shared` XCFramework and writes the API keys out of
`local.properties`. Build phases do both on every build afterwards, but the first
build needs them already there: `xcodebuild` resolves the framework reference and
the source file list before any phase runs.

Then open `ios/Lexicon.xcodeproj`, or from the command line:

```bash
xcodebuild -project ios/Lexicon.xcodeproj -scheme Lexicon -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath ios/build build
```

`SKIP_GRADLE=1` skips the framework rebuild when only Swift has changed, which is
most of the time and saves several minutes.

## How it is put together

**The Swift side owns the presentation, the Kotlin side owns everything else.** A
SwiftUI view has a small `ObservableObject` beside it that calls shared use cases;
the logic those use cases run is the same code the Android app runs. Screen state is
therefore written twice, once per platform, and nothing below it is.

**`IosDependencies` is the only way in.** Koin's `get<T>()` needs a reified type
parameter, which does not survive into Objective-C, so every use case the app needs
is named as a property on that object in `shared/src/iosMain`. Adding a screen that
needs a new use case means adding a line there first.

**Kotlin flows are wrapped, not collected.** A `Flow` reaches Swift as an opaque
type with no way to collect it. `IosDependencies.watchX(onEach:)` wraps each one the
app watches and hands back a `Cancellable` for the view to cancel.

**The catalogues are copied, not duplicated.** A build phase copies
`data/src/androidMain/assets/*.json` into the bundle, where `AssetReader.ios` looks
them up by name. One source of truth, read by both platforms.

**Files are picked up from the folder.** The project uses a file-system synchronized
group, so a new `.swift` file under `Lexicon/` needs no project edit.

## What is not here yet

- **DeepL.** Translation goes corpus first, then MyMemory, which needs no key.
  Android also tries DeepL; adding it here is another `RemoteImageSource`-shaped
  class in `iosMain`.
- **Unsplash**, for the same reason — Pexels, Pixabay and Openverse are wired.
