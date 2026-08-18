package com.lexicon.android.audio

interface AudioPlayer {
    suspend fun play(filePath: String)
}

class AudioPlaybackFailed(message: String) : Exception(message)
