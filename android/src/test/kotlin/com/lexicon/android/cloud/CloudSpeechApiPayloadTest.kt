package com.lexicon.android.cloud

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards a failure that says nothing when it happens.
 *
 * kotlinx.serialization drops defaulted fields unless `encodeDefaults` is set, and the
 * request still serialises, still sends, and still comes back — as a rejection. The first
 * symptom is every voice sounding identical, which points nowhere near the serializer.
 */
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
