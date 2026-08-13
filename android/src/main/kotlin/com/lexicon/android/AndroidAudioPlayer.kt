package com.lexicon.android

import android.media.MediaPlayer
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidAudioPlayer(
    private val dispatchers: DispatcherProvider,
) : AudioPlayer {
    override suspend fun play(filePath: String) {
        withContext(dispatchers.io) {
            suspendCancellableCoroutine { continuation ->
                val player = MediaPlayer()
                player.setOnCompletionListener {
                    it.release()
                    if (continuation.isActive) continuation.resume(Unit)
                }
                player.setOnErrorListener { mp, what, extra ->
                    mp.release()
                    if (continuation.isActive) {
                        continuation.resumeWithException(AudioPlaybackFailed("Playback failed (what=$what, extra=$extra)"))
                    }
                    true
                }
                continuation.invokeOnCancellation { player.release() }
                runCatching {
                    player.setDataSource(filePath)
                    player.prepare()
                    player.start()
                }.onFailure { failure ->
                    player.release()
                    if (continuation.isActive) {
                        continuation.resumeWithException(AudioPlaybackFailed(failure.message ?: "Playback failed"))
                    }
                }
            }
        }
    }
}
