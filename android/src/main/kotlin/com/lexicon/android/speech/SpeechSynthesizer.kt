package com.lexicon.android.speech

import java.util.Locale

enum class VoiceGender { FEMALE, MALE, NEUTRAL }

data class SpeechVoice(
    val id: String,
    val displayName: String,
    val gender: VoiceGender,
)

interface SpeechSynthesizer {
    suspend fun speak(
        text: String,
        locale: Locale = Locale.forLanguageTag("pl-PL"),
    )

    /** The Polish voices available to the learner, in a stable order. */
    suspend fun voices(): List<SpeechVoice>
}
