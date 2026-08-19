package com.lexicon.android.speech

import android.content.Context
import java.io.File
import java.security.MessageDigest

private const val SYNTHESIZE_DIR = ".synthesize"

private const val EXTENSION = ".mp3"

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

    private fun String.fileName(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) } + EXTENSION
}
