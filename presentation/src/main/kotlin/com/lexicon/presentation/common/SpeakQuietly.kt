package com.lexicon.presentation.common

import android.util.Log
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.runSuspendCatching

private const val TAG = "Speech"

suspend fun SpeechSynthesizer.speakQuietly(text: String) {
    runSuspendCatching { speak(text) }.onFailure { failure -> Log.w(TAG, "Speaking failed", failure) }
}
