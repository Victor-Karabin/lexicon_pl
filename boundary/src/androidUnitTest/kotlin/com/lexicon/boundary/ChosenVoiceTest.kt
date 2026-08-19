package com.lexicon.boundary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChosenVoiceTest {
    private fun voice(id: String) = SpeechVoice(id = id, displayName = id.uppercase(), gender = VoiceGender.NEUTRAL)

    private val voices = listOf(voice("agata"), voice("agnieszka"), voice("alicja"))

    @Test
    fun `the learner's own voice is used when it is on offer`() {
        assertEquals(voice("agnieszka"), voices.chosen("agnieszka"))
    }

    @Test
    fun `the first voice stands in when nothing has been chosen`() {
        assertEquals(voice("agata"), voices.chosen(null))
    }

    @Test
    fun `a choice that is no longer offered falls back to the first`() {
        assertEquals(voice("agata"), voices.chosen("a-voice-that-went-away"))
    }

    @Test
    fun `no voices means no choice`() {
        assertNull(emptyList<SpeechVoice>().chosen("agata"))
        assertNull(emptyList<SpeechVoice>().chosen(null))
    }

    @Test
    fun `the name shown and the row ticked are always the same voice`() {
        listOf(null, "", "agnieszka", "gone").forEach { stored ->
            val chosen = voices.chosen(stored)
            assertEquals("exactly one row may match for stored=$stored", 1, voices.count { it.id == chosen?.id })
        }
    }
}
