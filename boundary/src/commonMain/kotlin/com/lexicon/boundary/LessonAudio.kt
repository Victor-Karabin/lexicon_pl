package com.lexicon.boundary

import kotlinx.coroutines.flow.StateFlow

data class AudioPlayback(
    val file: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

interface LessonAudioPlayer {
    val playback: StateFlow<AudioPlayback?>

    suspend fun play(
        file: String,
        path: String,
        fromMs: Long? = null,
    )

    fun pause()

    fun seekTo(positionMs: Long)

    suspend fun durationOf(path: String): Long?

    fun stop()
}

interface LessonAudioLibrary {
    fun localPathOrNull(file: String): String?

    suspend fun pathOrNull(
        file: String,
        remoteId: String?,
    ): String?
}
