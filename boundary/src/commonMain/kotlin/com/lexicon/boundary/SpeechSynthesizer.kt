package com.lexicon.boundary

enum class VoiceGender { FEMALE, MALE, NEUTRAL }

data class SpeechVoice(
    val id: String,
    val displayName: String,
    val gender: VoiceGender,
)

/**
 * The voice the learner will actually hear: the one they chose, or the first on offer
 * when they have not chosen yet or their choice is no longer available.
 *
 * Everything that answers this question has to answer it the same way. When the settings
 * screen worked it out for its heading but compared the stored id for its radio buttons,
 * the heading named a voice and none of the buttons agreed with it.
 */
fun List<SpeechVoice>.chosen(preferredId: String?): SpeechVoice? = firstOrNull { it.id == preferredId } ?: firstOrNull()

interface SpeechSynthesizer {
    suspend fun speak(text: String)

    /** The Polish voices available to the learner, in a stable order. */
    suspend fun voices(): List<SpeechVoice>
}
