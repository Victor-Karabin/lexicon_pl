package com.lexicon.boundary

interface AudioPlayer {
    suspend fun play(filePath: String)
}

class AudioPlaybackFailed(message: String) : Exception(message)
