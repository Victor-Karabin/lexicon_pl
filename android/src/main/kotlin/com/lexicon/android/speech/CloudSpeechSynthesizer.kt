package com.lexicon.android.speech

import android.util.Log
import com.lexicon.android.cloud.CloudSpeechApi
import com.lexicon.boundary.AudioPlayer
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.boundary.SpeechVoice
import com.lexicon.boundary.chosen
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
                try {
                    nameVoices(api.voices(LANGUAGE_CODE))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Could not list Cloud voices", e)
                    emptyList()
                }
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
            try {
                player.play(path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Playing Cloud audio failed; speaking with the device voice instead", e)
                fallback.speak(text)
            }
        }
    }

    private suspend fun audioFor(text: String): String? {
        if (!api.isConfigured) return null
        val voice = chosenVoice() ?: return null

        store.filePath(voice, text)?.let { return it }

        val audio =
            try {
                api.synthesize(text, voice, LANGUAGE_CODE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Synthesis threw for $voice", e)
                null
            } ?: return null

        return store.store(voice, text, audio)
            ?: null.also { Log.w(TAG, "Could not keep the audio for $voice") }
    }

    private suspend fun chosenVoice(): String? = voices().chosen(settings.preferredVoiceId())?.id

    private companion object {
        private const val TAG = "CloudSpeechSynthesizer"
        private const val LANGUAGE_CODE = "pl-PL"
    }
}
