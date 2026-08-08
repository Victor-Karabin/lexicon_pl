package com.lexicon.android

import java.util.Locale

interface SpeechSynthesizer {
    suspend fun speak(
        text: String,
        locale: Locale = Locale.forLanguageTag("pl-PL"),
    )
}
