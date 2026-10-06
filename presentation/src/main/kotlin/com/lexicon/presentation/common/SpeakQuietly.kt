package com.lexicon.presentation.common

import android.util.Log
import com.lexicon.boundary.SpeechSynthesizer
import kotlinx.coroutines.CancellationException

private const val TAG = "Speech"

suspend fun SpeechSynthesizer.speakQuietly(text: String) {
    try {
        speak(text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Speaking failed", e)
    }
}
