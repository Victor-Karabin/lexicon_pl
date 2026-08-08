package com.lexicon.android

interface AudioPlayer {
    suspend fun play(filePath: String)
}

class AudioPlaybackFailed(message: String) : Exception(message)
