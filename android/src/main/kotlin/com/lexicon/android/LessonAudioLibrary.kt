package com.lexicon.android

import android.content.Context
import com.lexicon.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the course recordings live, and how they get there.
 *
 * The Krok po kroku audio is 454 MB, far past what belongs in an APK, so a track
 * arrives one of two ways: side-loaded in bulk by tools/course/install_audio.sh,
 * or fetched from the shared Drive folder the first time it is played. Either
 * way it ends up in the same directory, so a track is only ever downloaded once.
 *
 * The workbook recordings are not in the shared folder, so they carry no id and
 * remain side-load-only.
 */
@Singleton
class LessonAudioLibrary
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val httpClient: OkHttpClient,
        private val dispatchers: DispatcherProvider,
    ) {
        private val directory: File?
            get() = context.getExternalFilesDir(AUDIO_DIRECTORY)

        fun localPathOrNull(file: String): String? = directory?.resolve(file)?.takeIf { it.isFile }?.absolutePath

        fun availableFiles(): Set<String> = directory?.list()?.toSet().orEmpty()

        /**
         * A playable path for a track, downloading it if it is not here yet.
         * Null means the track cannot be played at all: no local copy, and either
         * no Drive id or a failed fetch.
         */
        suspend fun pathOrNull(
            file: String,
            remoteId: String?,
        ): String? {
            localPathOrNull(file)?.let { return it }
            if (remoteId == null) return null
            return download(file, remoteId)
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
