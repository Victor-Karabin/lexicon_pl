package com.lexicon.android

import java.util.Locale

data class SpeechVoice(
    val id: String,
)

interface SpeechSynthesizer {
    suspend fun speak(
        text: String,
        locale: Locale = Locale.forLanguageTag("pl-PL"),
    )

    /** The distinct Polish voices this device has, in a stable order. */
    suspend fun voices(): List<SpeechVoice>
}
