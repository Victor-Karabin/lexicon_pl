package com.lexicon.presentation.course

import android.util.Log
import com.lexicon.boundary.AudioPlayback
import com.lexicon.boundary.LessonAudioLibrary
import com.lexicon.boundary.LessonAudioPlayer
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LessonTracks"

val playbackSpeeds = listOf(0.5f, 1f, 1.5f, 2f)

data class TrackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
)

data class TrackStates(
    private val durations: Map<String, Long> = emptyMap(),
    private val positions: Map<String, Long> = emptyMap(),
    private val playback: AudioPlayback? = null,
    private val speed: Float = 1f,
) {
    val activeFile: String? get() = playback?.file

    fun of(file: String): TrackState {
        val live = playback?.takeIf { it.file == file }
        return TrackState(
            isPlaying = live?.isPlaying == true,
            positionMs = live?.positionMs ?: positions[file] ?: 0,
            durationMs = live?.durationMs?.takeIf { it > 0 } ?: durations[file] ?: 0,
            speed = speed,
        )
    }
}

class LessonTracks(
    private val library: LessonAudioLibrary,
    private val player: LessonAudioPlayer,
    private val dispatchers: DispatcherProvider,
) {
    private data class Known(
        val durations: Map<String, Long> = emptyMap(),
        val positions: Map<String, Long> = emptyMap(),
    )

    private val known = MutableStateFlow(Known())

    val states: Flow<TrackStates> =
        combine(known, player.playback, player.speed) { known, playback, speed ->
            TrackStates(known.durations, known.positions, playback, speed)
        }

    fun nextSpeed() {
        val at = playbackSpeeds.indexOf(player.speed.value)
        player.setSpeed(playbackSpeeds[(at + 1) % playbackSpeeds.size])
    }

    fun preload(
        scope: CoroutineScope,
        file: String,
        remoteId: String?,
    ) {
        if (file in known.value.durations) return
        scope.launch(dispatchers.io) {
            val path = library.pathOrNull(file, remoteId) ?: return@launch
            val duration = runSuspendCatching { player.durationOf(path) }.getOrNull() ?: return@launch
            known.update { it.copy(durations = it.durations + (file to duration)) }
        }
    }

    fun toggle(
        scope: CoroutineScope,
        file: String,
        remoteId: String?,
    ) {
        val playback = player.playback.value?.takeIf { it.file == file }
        if (playback?.isPlaying == true) {
            player.pause()
            return
        }
        scope.launch(dispatchers.io) {
            val path = library.pathOrNull(file, remoteId) ?: return@launch
            val from = if (playback == null) known.value.positions[file] else null
            runSuspendCatching { player.play(file, path, from) }
                .onSuccess { known.update { it.copy(positions = it.positions - file) } }
                .onFailure { Log.w(TAG, "A lesson recording could not be played", it) }
        }
    }

    fun seek(
        file: String,
        positionMs: Long,
    ) {
        if (player.playback.value?.file == file) {
            player.seekTo(positionMs)
        } else {
            known.update { it.copy(positions = it.positions + (file to positionMs)) }
        }
    }

    fun stop() = player.stop()
}
