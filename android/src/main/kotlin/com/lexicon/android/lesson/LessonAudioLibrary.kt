package com.lexicon.android.lesson

interface LessonAudioLibrary {
    fun localPathOrNull(file: String): String?

    fun availableFiles(): Set<String>

    suspend fun pathOrNull(
        file: String,
        remoteId: String?,
    ): String?
}
