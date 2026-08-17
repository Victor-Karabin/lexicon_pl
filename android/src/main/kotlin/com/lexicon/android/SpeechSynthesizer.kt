package com.lexicon.android

import java.util.Locale

data class SpeechVoice(
    val id: String,
    val displayName: String,
)

interface SpeechSynthesizer {
    suspend fun speak(
        text: String,
        locale: Locale = Locale.forLanguageTag("pl-PL"),
    )

    /** The Polish voices this device has, named so one can be chosen from a list. */
    suspend fun voices(): List<SpeechVoice>
}
