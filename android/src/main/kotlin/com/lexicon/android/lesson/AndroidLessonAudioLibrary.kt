package com.lexicon.android.lesson

import android.content.Context
import com.lexicon.boundary.LessonAudioLibrary
import com.lexicon.common.DispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class AndroidLessonAudioLibrary(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : LessonAudioLibrary {
    private val directory: File?
        get() = context.getExternalFilesDir(AUDIO_DIRECTORY)

    private val downloadMutexes = ConcurrentHashMap<String, Mutex>()

    override fun localPathOrNull(file: String): String? = directory?.resolve(file)?.takeIf { it.isFile }?.absolutePath

    override fun availableFiles(): Set<String> = directory?.list()?.toSet().orEmpty()

    override suspend fun pathOrNull(
        file: String,
        remoteId: String?,
    ): String? {
        localPathOrNull(file)?.let { return it }
        if (remoteId == null) return null
        return downloadMutexes.getOrPut(file) { Mutex() }.withLock {
            localPathOrNull(file)?.let { return@withLock it }
            download(file, remoteId)
        }
    }

    private suspend fun download(
        file: String,
        remoteId: String,
    ): String? =
        withContext(dispatchers.io) {
            val target = directory?.resolve(file) ?: return@withContext null
            target.parentFile?.mkdirs()

            val partial = File("${target.absolutePath}.part")
            runCatching {
                val request = Request.Builder().url(DOWNLOAD_URL.format(remoteId)).build()
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful || body == null) return@use null
                    partial.outputStream().use { out -> body.byteStream().copyTo(out) }
                    if (partial.renameTo(target)) target.absolutePath else null
                }
            }.getOrNull().also { if (it == null) partial.delete() }
        }

    private companion object {
        const val AUDIO_DIRECTORY = "lesson_audio"

        const val DOWNLOAD_URL = "https://drive.usercontent.google.com/download?id=%s&export=download"
    }
}
