package com.lexicon.interactors.course

data class LessonAudio(
    val file: String,
    val section: String?,
    val task: Int,
    val part: String?,
    val remoteId: String?,
)

val LessonAudio.label: String
    get() = buildString {
        section?.let { append(it) }
        append(task)
        part?.let { append('.').append(it) }
    }
