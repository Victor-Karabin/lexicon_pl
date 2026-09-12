package com.lexicon.android.speech

import android.content.Context
import java.io.File
import java.security.MessageDigest

private const val SYNTHESIZE_DIR = ".synthesize"

private const val EXTENSION = ".mp3"

private const val PARTIAL_SUFFIX = ".part"

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
    private val root: File,
) : SpeechStore {
    constructor(context: Context) : this(File(context.filesDir, SYNTHESIZE_DIR))

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
        val partial = File(file.parentFile, file.name + PARTIAL_SUFFIX)
        return runCatching {
            partial.writeBytes(audio)
            check(partial.renameTo(file)) { "could not move ${partial.name} into place" }
            file.absolutePath
        }.onFailure { partial.delete() }.getOrNull()
    }

    private fun file(
        voice: String,
        text: String,
    ): File? {
        val directory = File(root, voice)
        if (!directory.isDirectory && !directory.mkdirs()) return null
        return File(directory, text.fileName())
    }

    private fun String.fileName(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) } + EXTENSION
}
