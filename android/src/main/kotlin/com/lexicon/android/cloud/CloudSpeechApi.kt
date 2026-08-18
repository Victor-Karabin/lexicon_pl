package com.lexicon.android.cloud

import android.util.Base64
import android.util.Log
import com.lexicon.android.speech.VoiceGender
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val BASE_URL = "https://texttospeech.googleapis.com/v1"

private const val SPEECH_URL = "https://speech.googleapis.com/v1/speech:recognize"

private const val AUDIO_ENCODING = "MP3"

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

/** A Cloud voice as the service describes it, gender included — which is the whole point. */
data class CloudVoice(
    val name: String,
    val languageCode: String,
    val gender: VoiceGender,
)

@Serializable
private data class VoicesResponse(
    val voices: List<VoiceJson> = emptyList(),
)

@Serializable
private data class VoiceJson(
    val name: String = "",
    val languageCodes: List<String> = emptyList(),
    val ssmlGender: String = "",
)

@Serializable
internal data class SynthesizeRequest(
    val input: InputJson,
    val voice: VoiceSelectionJson,
    val audioConfig: AudioConfigJson,
)

@Serializable
internal data class InputJson(val text: String)

@Serializable
internal data class VoiceSelectionJson(
    val languageCode: String,
    val name: String,
)

@Serializable
internal data class AudioConfigJson(
    @SerialName("audioEncoding") val encoding: String = AUDIO_ENCODING,
)

@Serializable
private data class SynthesizeResponse(
    val audioContent: String = "",
)

@Serializable
private data class RecognizeRequest(
    val config: RecognitionConfigJson,
    val audio: RecognitionAudioJson,
)

@Serializable
private data class RecognitionConfigJson(
    val encoding: String,
    val sampleRateHertz: Int,
    val languageCode: String,
    val model: String,
)

@Serializable
private data class RecognitionAudioJson(val content: String)

@Serializable
private data class RecognizeResponse(
    val results: List<RecognitionResultJson> = emptyList(),
)

@Serializable
private data class RecognitionResultJson(
    val alternatives: List<AlternativeJson> = emptyList(),
)

@Serializable
private data class AlternativeJson(
    val transcript: String = "",
    val confidence: Float? = null,
)

/** What the recogniser made of a recording. */
data class CloudTranscript(
    val text: String,
    val confidence: Float?,
)

/**
 * Google Cloud Text-to-Speech over its REST interface.
 *
 * REST rather than the Cloud client library: the library is Android-only and heavy, and
 * this project has an iOS target to keep alive. A plain request costs one dependency
 * already on hand.
 */
class CloudSpeechApi(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val json = CLOUD_JSON

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    fun voices(languageCode: String): List<CloudVoice> {
        val request = Request.Builder()
            .url("$BASE_URL/voices?languageCode=$languageCode&key=$apiKey")
            .get()
            .build()

        val body = client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "Listing voices failed: ${response.code} ${payload.take(ERROR_LOG_LIMIT)}")
                return emptyList()
            }
            payload
        }

        return json
            .decodeFromString(VoicesResponse.serializer(), body)
            .voices
            .map {
                CloudVoice(
                    name = it.name,
                    languageCode = it.languageCodes.firstOrNull() ?: languageCode,
                    gender = it.ssmlGender.toGender(),
                )
            }
    }

    fun synthesize(
        text: String,
        voice: String,
        languageCode: String,
    ): ByteArray? {
        val payload = synthesizePayload(text = text, voice = voice, languageCode = languageCode)

        val request = Request.Builder()
            .url("$BASE_URL/text:synthesize?key=$apiKey")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val body = client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "Synthesis failed for $voice: ${response.code} ${payload.take(ERROR_LOG_LIMIT)}")
                return null
            }
            payload
        }

        val encoded = json.decodeFromString(SynthesizeResponse.serializer(), body).audioContent
        if (encoded.isBlank()) {
            Log.e(TAG, "Synthesis returned no audio for $voice")
            return null
        }
        return Base64.decode(encoded, Base64.DEFAULT)
    }

    /**
     * What Cloud hears in a recording.
     *
     * The header is dropped and the format stated outright rather than left to be sniffed,
     * because the recorder already knows exactly what it wrote.
     */
    fun recognize(
        wav: ByteArray,
        languageCode: String,
        sampleRateHertz: Int,
    ): CloudTranscript? {
        val pcm = if (wav.size > WAV_HEADER_SIZE) wav.copyOfRange(WAV_HEADER_SIZE, wav.size) else return null

        val payload = json.encodeToString(
            RecognizeRequest.serializer(),
            RecognizeRequest(
                config = RecognitionConfigJson(
                    encoding = "LINEAR16",
                    sampleRateHertz = sampleRateHertz,
                    languageCode = languageCode,
                    model = RECOGNITION_MODEL,
                ),
                audio = RecognitionAudioJson(Base64.encodeToString(pcm, Base64.NO_WRAP)),
            ),
        )

        val request = Request.Builder()
            .url("$SPEECH_URL?key=$apiKey")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val body = client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "Recognition failed: ${response.code} ${payload.take(ERROR_LOG_LIMIT)}")
                return null
            }
            payload
        }

        val best = json
            .decodeFromString(RecognizeResponse.serializer(), body)
            .results
            .firstNotNullOfOrNull { it.alternatives.firstOrNull() }
            ?: return CloudTranscript(text = "", confidence = null)

        return CloudTranscript(text = best.transcript.trim(), confidence = best.confidence)
    }

    private companion object {
        private const val TAG = "CloudSpeechApi"

        /** Enough of a refusal to name it, without pasting a whole response into the log. */
        private const val ERROR_LOG_LIMIT = 300

        private const val WAV_HEADER_SIZE = 44

        /** Tuned for short commands and single phrases, which is what a learner reads out. */
        private const val RECOGNITION_MODEL = "latest_short"
    }
}

/**
 * `encodeDefaults` matters here, and its absence is silent.
 *
 * kotlinx.serialization leaves defaulted fields out of the output unless told otherwise,
 * so `audioConfig` went over the wire as `{}` and the encoding — the only field it
 * carries — never arrived. Synthesis then failed for every voice alike, the synthesiser
 * fell back to the device, and every voice in the list came out sounding the same.
 */
private val CLOUD_JSON = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** The body of a synthesis request, built where a test can read it. */
internal fun synthesizePayload(
    text: String,
    voice: String,
    languageCode: String,
): String =
    CLOUD_JSON.encodeToString(
        SynthesizeRequest.serializer(),
        SynthesizeRequest(
            input = InputJson(text),
            voice = VoiceSelectionJson(languageCode = languageCode, name = voice),
            audioConfig = AudioConfigJson(),
        ),
    )

private fun String.toGender(): VoiceGender =
    when (uppercase()) {
        "FEMALE" -> VoiceGender.FEMALE
        "MALE" -> VoiceGender.MALE
        else -> VoiceGender.NEUTRAL
    }
