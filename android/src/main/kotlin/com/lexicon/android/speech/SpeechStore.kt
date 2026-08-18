package com.lexicon.android.speech

import android.content.Context
import java.io.File
import java.security.MessageDigest

private const val SYNTHESIZE_DIR = ".synthesize"

private const val EXTENSION = ".mp3"

/**
 * Where synthesised speech is kept between runs.
 *
 * Cloud synthesis is billed by the character and needs the network, but a vocabulary app
 * says the same few hundred words over and over. Kept under `filesDir` rather than the
 * cache directory so Android cannot quietly reclaim it: a phrase is paid for once and
 * then belongs to the device, which also means everything already heard still plays with
 * no connection.
 */
interface SpeechStore {
    fun filePath(
        voice: String,
        text: String,
    ): String?

    fun store(
        voice: String,
        text: String,
        audio: ByteArray,
    ): String?
}

class AndroidSpeechStore(
    private val context: Context,
) : SpeechStore {
    override fun filePath(
        voice: String,
        text: String,
    ): String? = file(voice, text)?.takeIf { it.isFile && it.length() > 0 }?.absolutePath

    override fun store(
        voice: String,
        text: String,
        audio: ByteArray,
    ): String? {
        val file = file(voice, text) ?: return null
        return runCatching { file.apply { writeBytes(audio) }.absolutePath }.getOrNull()
    }

    private fun file(
        voice: String,
        text: String,
    ): File? {
        val directory = File(File(context.filesDir, SYNTHESIZE_DIR), voice)
        if (!directory.isDirectory && !directory.mkdirs()) return null
        return File(directory, text.fileName())
    }

    /**
     * The text digested rather than used as it stands.
     *
     * The old app named the file after the phrase, which held up while phrases were single
     * words. Whole sentences carry punctuation and slashes and run past the filesystem's
     * name limit, so the phrase is hashed instead.
     */
    private fun String.fileName(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) } + EXTENSION
}
