package com.lexicon.android.speech

/** Which voice the learner chose, read straight before speaking so a change takes at once. */
fun interface VoicePreference {
    suspend fun preferredVoiceId(): String?
}
