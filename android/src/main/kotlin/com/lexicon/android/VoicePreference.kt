package com.lexicon.android

/** Which voice the learner chose, read straight before speaking so a change takes at once. */
fun interface VoicePreference {
    suspend fun preferredVoiceId(): String?
}
