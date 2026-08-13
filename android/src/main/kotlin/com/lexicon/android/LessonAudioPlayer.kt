package com.lexicon.android

import kotlinx.coroutines.flow.StateFlow

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
interface LessonAudioPlayer {
    /** The track currently playing, or null when nothing is. */
    val playingFile: StateFlow<String?>

    suspend fun play(
        file: String,
        path: String,
    )

    fun pause()

    fun stop()
}
