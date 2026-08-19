package com.lexicon.android.lesson

import android.media.MediaPlayer
import com.lexicon.boundary.LessonAudioPlayer
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AndroidLessonAudioPlayer(
    private val dispatchers: DispatcherProvider,
) : LessonAudioPlayer {
    private var player: MediaPlayer? = null
    private var loadedFile: String? = null

    private val lock = Any()

    private val _playingFile = MutableStateFlow<String?>(null)
    override val playingFile: StateFlow<String?> = _playingFile.asStateFlow()

    override suspend fun play(
        file: String,
        path: String,
    ) {
        withContext(dispatchers.io) {
            synchronized(lock) {
                if (loadedFile != file) reset(file, path)
                player?.run {
                    seekTo(0)
                    start()
                }
            }
            _playingFile.value = file
        }
    }

    override fun pause() {
        synchronized(lock) { runCatching { player?.takeIf { it.isPlaying }?.pause() } }
        _playingFile.value = null
    }

    override fun stop() {
        synchronized(lock) {
            runCatching { player?.release() }
            player = null
            loadedFile = null
        }
        _playingFile.value = null
    }

    private fun reset(
        file: String,
        path: String,
    ) {
        runCatching { player?.release() }
        player =
            MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener { _playingFile.value = null }
            }
        loadedFile = file
    }
}
