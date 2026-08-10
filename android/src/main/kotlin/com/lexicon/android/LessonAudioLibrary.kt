package com.lexicon.android

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the course recordings live on the device.
 *
 * The Krok po kroku audio is 454 MB, far past what belongs in an APK, so the files
 * are side-loaded into app-specific external storage instead (see
 * tools/course/install_audio.sh). Every lookup can therefore come back empty, and
 * the lesson screen treats that as "no recording" rather than an error: the audio
 * is an extra on top of a lesson that works without it.
 */
@Singleton
class LessonAudioLibrary
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val directory: File?
            get() = context.getExternalFilesDir(AUDIO_DIRECTORY)

        fun pathOrNull(file: String): String? = directory?.resolve(file)?.takeIf { it.isFile }?.absolutePath

        fun availableFiles(): Set<String> = directory?.list()?.toSet().orEmpty()

        private companion object {
            const val AUDIO_DIRECTORY = "lesson_audio"
        }
    }
