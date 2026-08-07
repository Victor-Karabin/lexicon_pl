package com.lexicon.android

/** Plays back a locally cached audio file, e.g. the user's own recorded pronunciation attempt. */
interface AudioPlayer {
    suspend fun play(filePath: String)
}

class AudioPlaybackFailed(message: String) : Exception(message)
