package com.lexicon.android.speech

import android.util.Log
import com.lexicon.android.cloud.CloudSpeechApi
import com.lexicon.boundary.AudioPlayer
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.boundary.SpeechVoice
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Speaks with Google Cloud voices, and with the device's own when it cannot.
 *
 * Cloud is the one that carries gender and sounds like a person, so it leads. It also
 * needs a key, a network and a working service, none of which are guaranteed — so every
 * path that fails falls through to the device synthesiser rather than leaving the learner
 * with silence. Anything heard once is on disk and plays again without either.
 */
class CloudSpeechSynthesizer(
    private val api: CloudSpeechApi,
    private val store: SpeechStore,
    private val player: AudioPlayer,
    private val settings: VoicePreference,
    private val fallback: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : SpeechSynthesizer {
    private val lock = Mutex()
    private var cached: List<SpeechVoice>? = null

    override suspend fun voices(): List<SpeechVoice> {
        if (!api.isConfigured) return fallback.voices()

        val cloud = lock.withLock {
            cached ?: withContext(dispatchers.io) {
                runCatching { nameVoices(api.voices(LANGUAGE_CODE)) }
                    .onFailure { failure -> Log.e(TAG, "Could not list Cloud voices", failure) }
                    .getOrDefault(emptyList())
            }.also { if (it.isNotEmpty()) cached = it }
        }

        return cloud.ifEmpty { fallback.voices() }
    }

    override suspend fun speak(text: String) {
        if (text.isBlank()) return
        val path = withContext(dispatchers.io) { audioFor(text) }

        if (path == null) {
            Log.w(TAG, "No Cloud audio; speaking with the device voice instead")
            fallback.speak(text)
        } else {
            runCatching { player.play(path) }.onFailure { failure ->
                Log.w(TAG, "Playing Cloud audio failed; speaking with the device voice instead", failure)
                fallback.speak(text)
            }
        }
    }

    /** The file to play, fetching and keeping it first if this phrase is new. */
    private suspend fun audioFor(text: String): String? {
        if (!api.isConfigured) return null
        val voice = chosenVoice() ?: return null

        store.filePath(voice, text)?.let { return it }

        val audio = runCatching { api.synthesize(text, voice, LANGUAGE_CODE) }
            .onFailure { failure -> Log.e(TAG, "Synthesis threw for $voice", failure) }
            .getOrNull() ?: return null

        return store.store(voice, text, audio)
            ?: null.also { Log.w(TAG, "Could not keep the audio for $voice") }
    }

    /**
     * The learner's voice, or the first one going.
     *
     * A stored id from the device synthesiser will not name a Cloud voice, so it is only
     * honoured when Cloud actually offers it.
     */
    private suspend fun chosenVoice(): String? {
        val available = voices().map { it.id }
        if (available.isEmpty()) return null
        return settings.preferredVoiceId()?.takeIf { it in available } ?: available.first()
    }

    private companion object {
        private const val TAG = "CloudSpeechSynthesizer"
        private const val LANGUAGE_CODE = "pl-PL"
    }
}
