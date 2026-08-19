package com.lexicon.android.speech

fun interface VoicePreference {
    suspend fun preferredVoiceId(): String?
}
