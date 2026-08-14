package com.lexicon.android

/**
 * Where the course recordings live, and how they get there.
 *
 * The Krok po kroku audio is 454 MB, far past what belongs in an APK, so a track
 * arrives one of two ways: side-loaded in bulk by tools/course/install_audio.sh,
 * or fetched from the shared Drive folder the first time it is played. Either
 * way it ends up in the same directory, so a track is only ever downloaded once.
 *
 * The workbook recordings are not in the shared folder, so they carry no id and
 * remain side-load-only.
 */
interface LessonAudioLibrary {
    /** The path of an already-downloaded track, or null if it is not here yet. */
    fun localPathOrNull(file: String): String?

    fun availableFiles(): Set<String>

    /**
     * A playable path for a track, downloading it if it is not here yet.
     * Null means the track cannot be played at all: no local copy, and either
     * no Drive id or a failed fetch.
     */
    suspend fun pathOrNull(
        file: String,
        remoteId: String?,
    ): String?
}
