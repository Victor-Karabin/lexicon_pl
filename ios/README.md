# Lexicon for iOS

The SwiftUI app. It shares everything below the UI with Android — use cases,
repositories, the Room database, the review schedule — through the `Shared`
XCFramework the `:shared` Gradle module produces.

## Running it

The app links `../shared/build/XCFrameworks/release/Shared.xcframework`, which does
not exist in a clean checkout. Xcode builds it as the target's first build phase,
but the very first build has to be told to make it, because `xcodebuild` resolves
the framework reference before any phase runs:

```bash
./gradlew :shared:assembleSharedReleaseXCFramework
```

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

- **Pictures and remote translation.** `dataIosModule` binds no `RemoteImageSource`
  and only the offline translator: the image and DeepL clients are Retrofit-based
  and Android-only. Image Test, Puzzle, Memory Cards and Word Card therefore fall
  back to their text clues, and the new-word form fills the Polish side in from the
  corpus only. A Ktor or NSURLSession implementation in `iosMain` is what closes
  this, and needs no change above the data layer.
- **Lesson audio.** The recordings are fetched and played by the Android-only
  `:android` module; the lesson screen here shows the words and can train over them,
  but has no player.
- **Selection and swipe actions** in the vocabulary list, and the create-preset form.
