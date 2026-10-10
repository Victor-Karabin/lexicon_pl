package com.lexicon.android.lesson

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Log
import com.lexicon.boundary.AudioPlayback
import com.lexicon.boundary.LessonAudioPlayer
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TICK_MS = 250L
private const val TAG = "LessonAudioPlayer"

class AndroidLessonAudioPlayer(
    private val dispatchers: DispatcherProvider,
) : LessonAudioPlayer {
    private var player: MediaPlayer? = null
    private var loadedFile: String? = null
    private var ticker: Job? = null

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val _playback = MutableStateFlow<AudioPlayback?>(null)
    override val playback: StateFlow<AudioPlayback?> = _playback.asStateFlow()

    override suspend fun play(
        file: String,
        path: String,
        fromMs: Long?,
    ) {
        withContext(dispatchers.io) {
            synchronized(lock) {
                if (loadedFile != file) load(file, path)
                player?.run {
                    fromMs?.let { seekTo(it.toInt()) }
                    start()
                }
            }
            publish()
            startTicking()
        }
    }

    override fun pause() {
        synchronized(lock) { runCatching { player?.takeIf { it.isPlaying }?.pause() } }
        ticker?.cancel()
        publish()
    }

    override fun seekTo(positionMs: Long) {
        synchronized(lock) { runCatching { player?.seekTo(positionMs.toInt()) } }
        _playback.value = _playback.value?.copy(positionMs = positionMs)
    }

    override suspend fun durationOf(path: String): Long? =
        withContext(dispatchers.io) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "A recording's length could not be read", e)
                null
            } finally {
                retriever.release()
            }
        }

    override fun stop() {
        ticker?.cancel()
        synchronized(lock) {
            runCatching { player?.release() }
            player = null
            loadedFile = null
        }
        _playback.value = null
    }

    private fun load(
        file: String,
        path: String,
    ) {
        runCatching { player?.release() }
        player =
            MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    ticker?.cancel()
                    it.seekTo(0)
                    publish()
                }
            }
        loadedFile = file
    }

    private fun startTicking() {
        ticker?.cancel()
        ticker =
            scope.launch {
                while (isActive) {
                    publish()
                    delay(TICK_MS)
                }
            }
    }

    private fun publish() {
        _playback.value =
            synchronized(lock) {
                val current = player ?: return@synchronized null
                val file = loadedFile ?: return@synchronized null
                runCatching {
                    AudioPlayback(
                        file = file,
                        isPlaying = current.isPlaying,
                        positionMs = current.currentPosition.toLong(),
                        durationMs = current.duration.toLong().coerceAtLeast(0),
                    )
                }.getOrNull()
            }
    }
}
