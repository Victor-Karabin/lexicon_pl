package com.lexicon.pl.android

import java.util.Locale

/** Plays the pronunciation audio for a piece of text. Backed by the on-device TTS engine. */
interface SpeechSynthesizer {
    suspend fun speak(text: String, locale: Locale = Locale.forLanguageTag("pl-PL"))
}
