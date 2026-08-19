package com.lexicon.boundary

enum class VoiceGender { FEMALE, MALE, NEUTRAL }

data class SpeechVoice(
    val id: String,
    val displayName: String,
    val gender: VoiceGender,
)

fun List<SpeechVoice>.chosen(preferredId: String?): SpeechVoice? = firstOrNull { it.id == preferredId } ?: firstOrNull()

interface SpeechSynthesizer {
    suspend fun speak(text: String)

    suspend fun voices(): List<SpeechVoice>
}
