package com.lexicon.boundary

import kotlinx.coroutines.flow.StateFlow

interface LessonAudioPlayer {
    val playingFile: StateFlow<String?>

    suspend fun play(
        file: String,
        path: String,
    )

    fun pause()

    fun stop()
}

interface LessonAudioLibrary {
    fun localPathOrNull(file: String): String?

    suspend fun pathOrNull(
        file: String,
        remoteId: String?,
    ): String?
}
