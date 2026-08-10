#!/usr/bin/env bash
# Side-loads the Krok po kroku recordings onto a connected device.
#
# The 488 tracks are 454 MB, so they are never bundled in the APK. This unpacks them
# from the source archives into the local cache and pushes them to the app's own
# external files directory, where LessonAudioLibrary looks for them.
#
#   ./tools/course/install_audio.sh [applicationId]
#
# Lessons work without this; the audio controls simply stay disabled.
set -euo pipefail

APPLICATION_ID="${1:-com.lexicon}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACHE="$(cd "$HERE/../.." && pwd)/../krok/.cache/audio"
TARGET="/sdcard/Android/data/${APPLICATION_ID}/files/lesson_audio"

if ! command -v adb >/dev/null; then
  echo "adb not found; install the Android platform tools" >&2
  exit 1
fi

echo "Unpacking audio into $CACHE"
python3 "$HERE/extract_audio.py" --unpack

echo "Pushing to $TARGET"
adb shell mkdir -p "$TARGET"
for book in "$CACHE"/*/; do
  echo "  $(basename "$book")"
  adb push "$book". "$TARGET/" >/dev/null
done

echo "Installed $(adb shell ls "$TARGET" | wc -l | tr -d ' ') tracks"
