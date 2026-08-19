package com.lexicon.android.cloud

import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSpeechApiPayloadTest {
    private val payload = synthesizePayload(
        text = "Dzień dobry",
        voice = "pl-PL-Chirp3-HD-Kore",
        languageCode = "pl-PL",
    )

    @Test
    fun `the audio encoding survives serialisation`() {
        assertTrue(payload, payload.contains("\"audioEncoding\":\"MP3\""))
    }

    @Test
    fun `the audio config is never sent empty`() {
        assertTrue(payload, !payload.contains("\"audioConfig\":{}"))
    }

    @Test
    fun `the chosen voice is what gets asked for`() {
        assertTrue(payload, payload.contains("\"name\":\"pl-PL-Chirp3-HD-Kore\""))
        assertTrue(payload, payload.contains("\"languageCode\":\"pl-PL\""))
    }

    @Test
    fun `the text to speak is carried through`() {
        assertTrue(payload, payload.contains("Dzień dobry"))
    }
}
