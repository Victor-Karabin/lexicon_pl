package com.lexicon.android.lesson

import android.content.Context
import android.util.Log
import com.lexicon.boundary.LessonAudioLibrary
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
            var saved: String? = null
            try {
                saved = runSuspendCatching {
                    val request = Request.Builder().url(DOWNLOAD_URL.format(remoteId)).build()
                    httpClient.newCall(request).execute().use { response -> save(file, response, partial, target) }
                }.onFailure { Log.w(TAG, "The recording $file could not be downloaded", it) }.getOrNull()
            } finally {
                if (saved == null) partial.delete()
            }
            saved
        }

    private fun save(
        file: String,
        response: Response,
        partial: File,
        target: File,
    ): String? {
        val body = response.body
        if (!response.isSuccessful || body == null) {
            Log.w(TAG, "The recording $file came back with HTTP ${response.code}")
            return null
        }
        val type = body.contentType()
        if (type?.type != AUDIO_TYPE) {
            Log.w(TAG, "The recording $file came back as $type, not audio")
            return null
        }
        partial.outputStream().use { out -> body.byteStream().copyTo(out) }
        return target.absolutePath.takeIf { partial.renameTo(target) }
    }

    private companion object {
        const val TAG = "LessonAudioLibrary"

        const val AUDIO_TYPE = "audio"

        const val AUDIO_DIRECTORY = "lesson_audio"

        const val DOWNLOAD_URL = "https://drive.usercontent.google.com/download?id=%s&export=download"
    }
}
