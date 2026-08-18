package com.lexicon.android.speech

import com.lexicon.android.audio.AudioPlayer
import com.lexicon.android.cloud.CloudSpeechApi
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

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
                runCatching { nameVoices(api.voices(LANGUAGE_CODE)) }.getOrDefault(emptyList())
            }.also { if (it.isNotEmpty()) cached = it }
        }

        return cloud.ifEmpty { fallback.voices() }
    }

    override suspend fun speak(
        text: String,
        locale: Locale,
    ) {
        if (text.isBlank()) return
        val path = withContext(dispatchers.io) { audioFor(text) }

        if (path == null) {
            fallback.speak(text, locale)
        } else {
            runCatching { player.play(path) }.onFailure { fallback.speak(text, locale) }
        }
    }

    /** The file to play, fetching and keeping it first if this phrase is new. */
    private suspend fun audioFor(text: String): String? {
        if (!api.isConfigured) return null
        val voice = chosenVoice() ?: return null

        store.filePath(voice, text)?.let { return it }

        val audio = runCatching { api.synthesize(text, voice, LANGUAGE_CODE) }.getOrNull() ?: return null
        return store.store(voice, text, audio)
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
        private const val LANGUAGE_CODE = "pl-PL"
    }
}
