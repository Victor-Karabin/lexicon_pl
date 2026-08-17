package com.lexicon.android

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
