package com.lexicon.android

import android.content.Context
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

    // One mutex per file, so two callers racing to play the same not-yet-cached
    // track serialize onto a single download instead of both writing the same
    // .part file.
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
            // Re-check: another caller may have finished downloading this file
            // while this one waited for the lock.
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

            // Downloaded beside the target and renamed, so an interrupted fetch
            // cannot leave a half-file that later looks like a cached track.
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

        // Drive serves the bytes directly at this endpoint for files this size;
        // the virus-scan interstitial only appears well above 100 MB.
        const val DOWNLOAD_URL = "https://drive.usercontent.google.com/download?id=%s&export=download"
    }
}
