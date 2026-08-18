package com.lexicon.android.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

internal const val RECORDING_SAMPLE_RATE_HZ = 16_000

private const val BITS_PER_SAMPLE = 16
private const val CHANNELS = 1
private const val WAV_HEADER_SIZE = 44

/** Loudness, on the same 0..1 scale as a full-scale sample, above which someone is talking. */
private const val SPEECH_LEVEL = 0.02

/** Silence this long after speech means the phrase is over. */
private const val TRAILING_SILENCE_MS = 1_200

/** How long to wait for someone to start before giving up. */
private const val PATIENCE_MS = 6_000

/** A hard stop, so a noisy room cannot record forever. */
private const val MAX_LENGTH_MS = 20_000

/**
 * Records a phrase from the microphone and hands back the file.
 *
 * The platform recogniser will not share its audio — `onBufferReceived` is optional and
 * Google's implementation never calls it — so anything that needs the recording itself,
 * whether to send for recognition or to play back, has to capture it here.
 */
interface AudioRecorder {
    suspend fun record(): String?
}

private const val TAG = "AudioRecorder"

class AndroidAudioRecorder(
    private val cacheDirectory: File,
    private val dispatchers: DispatcherProvider,
) : AudioRecorder {
    @SuppressLint("MissingPermission")
    override suspend fun record(): String? =
        withContext(dispatchers.io) {
            val minimum = AudioRecord.getMinBufferSize(
                RECORDING_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minimum <= 0) {
                Log.e(TAG, "The microphone reported no usable buffer size")
                return@withContext null
            }

            val recorder = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    RECORDING_SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minimum * 2,
                )
            }.onFailure { failure -> Log.e(TAG, "Could not open the microphone", failure) }
                .getOrNull() ?: return@withContext null

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "The microphone would not initialise")
                recorder.release()
                return@withContext null
            }

            val pcm = runCatching { recorder.capture(minimum) }
                .onFailure { failure -> Log.e(TAG, "Recording failed", failure) }
                .getOrNull()
            runCatching { recorder.stop() }
            recorder.release()

            if (pcm != null && pcm.isEmpty()) Log.w(TAG, "Nobody spoke before the recorder gave up")

            pcm?.takeIf { it.isNotEmpty() }?.let { writeWav(it) }
        }

    /** Reads until the speaker stops, they never start, or the hard limit is reached. */
    private fun AudioRecord.capture(bufferSize: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)
        val started = System.currentTimeMillis()
        var speechAt: Long? = null
        var quietSince: Long? = null

        startRecording()
        while (true) {
            val read = read(buffer, 0, buffer.size)
            if (read <= 0) break
            out.write(buffer, 0, read)

            val now = System.currentTimeMillis()
            val loud = buffer.level(read) > SPEECH_LEVEL

            when {
                loud -> {
                    if (speechAt == null) speechAt = now
                    quietSince = null
                }

                speechAt != null -> {
                    if (quietSince == null) quietSince = now
                    if (now - quietSince >= TRAILING_SILENCE_MS) break
                }

                now - started >= PATIENCE_MS -> break
            }

            if (now - started >= MAX_LENGTH_MS) break
        }

        return if (speechAt == null) ByteArray(0) else out.toByteArray()
    }

    private fun writeWav(pcm: ByteArray): String? {
        val file = File(cacheDirectory, "pronunciation_attempt_${System.currentTimeMillis()}.wav")
        return runCatching {
            file.outputStream().use { out ->
                out.writeWavHeader(pcm.size)
                out.write(pcm)
            }
            file.absolutePath
        }.onFailure { failure -> Log.e(TAG, "Could not write the recording", failure) }
            .getOrNull()
    }

    private fun OutputStream.writeWavHeader(dataSize: Int) {
        val byteRate = RECORDING_SAMPLE_RATE_HZ * CHANNELS * BITS_PER_SAMPLE / 8
        val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(CHANNELS.toShort())
        header.putInt(RECORDING_SAMPLE_RATE_HZ)
        header.putInt(byteRate)
        header.putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort())
        header.putShort(BITS_PER_SAMPLE.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize)
        write(header.array())
    }
}

/** Root-mean-square loudness of a chunk of 16-bit samples, as a fraction of full scale. */
private fun ByteArray.level(length: Int): Double {
    var sum = 0.0
    var count = 0
    var index = 0
    while (index + 1 < length) {
        val sample = ((this[index + 1].toInt() shl 8) or (this[index].toInt() and 0xFF)).toShort()
        sum += sample.toDouble() * sample.toDouble()
        count++
        index += 2
    }
    return if (count == 0) 0.0 else sqrt(sum / count) / Short.MAX_VALUE
}
