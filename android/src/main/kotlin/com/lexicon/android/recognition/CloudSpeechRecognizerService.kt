package com.lexicon.android.recognition

import com.lexicon.android.audio.AudioRecorder
import com.lexicon.android.audio.RECORDING_SAMPLE_RATE_HZ
import com.lexicon.android.cloud.CloudSpeechApi
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Judges pronunciation with Google Cloud, and with the device's recogniser when it cannot.
 *
 * The platform recogniser and Cloud cannot share a turn at the microphone, so the choice
 * is made before recording rather than after: with a key, this records the phrase itself
 * and sends it; without one, the platform handles the whole thing as before. Recording it
 * here also means there is finally an audio file to play back, which the platform
 * recogniser only pretended to provide.
 */
class CloudSpeechRecognizerService(
    private val recorder: AudioRecorder,
    private val api: CloudSpeechApi,
    private val device: SpeechRecognizerService,
    private val dispatchers: DispatcherProvider,
) : SpeechRecognizerService {
    override suspend fun recognize(locale: Locale): SpeechRecognitionResult {
        if (!api.isConfigured) return device.recognize(locale)

        val path = recorder.record() ?: throw SpeechRecognitionFailed("Nothing was recorded")

        val transcript = withContext(dispatchers.io) {
            runCatching { api.recognize(File(path).readBytes(), locale.toLanguageTag(), RECORDING_SAMPLE_RATE_HZ) }
                .getOrNull()
        }

        // Speaking again is the only way back from a failed call, since the recording has
        // already been made and the platform recogniser cannot be handed a file.
        transcript ?: throw SpeechRecognitionFailed("Speech could not be recognised just now")

        return SpeechRecognitionResult(
            recognizedText = transcript.text,
            confidence = transcript.confidence,
            audioFilePath = path,
        )
    }
}
