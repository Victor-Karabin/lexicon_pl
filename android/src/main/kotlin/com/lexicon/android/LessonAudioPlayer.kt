package com.lexicon.android

import android.media.MediaPlayer
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays one lesson recording at a time, and says which.
 *
 * [AudioPlayer] suspends until the clip ends and hands back no handle, which is
 * all a training needs. A lesson track is minutes long and the learner has to be
 * able to stop it, so this keeps the player around and exposes what is playing.
 *
 * Starting a track always plays from the beginning: pausing is a way to stop
 * listening, not a bookmark.
 */
@Singleton
class LessonAudioPlayer
    @Inject
    constructor(
        private val dispatchers: DispatcherProvider,
    ) {
        private var player: MediaPlayer? = null
        private var loadedFile: String? = null

        // play()/pause()/stop() can all be called from different callers at once (a
        // tap racing an onCleared(), or two tracks racing each other), and MediaPlayer
        // is not thread-safe — guards every access to player/loadedFile.
        private val lock = Any()

        private val _playingFile = MutableStateFlow<String?>(null)
        val playingFile: StateFlow<String?> = _playingFile.asStateFlow()

        suspend fun play(
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

        fun pause() {
            synchronized(lock) { runCatching { player?.takeIf { it.isPlaying }?.pause() } }
            _playingFile.value = null
        }

        fun stop() {
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
