package com.lexicon.android.speech

import com.lexicon.android.cloud.CloudVoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishVoiceNamesTest {
    private fun voice(
        name: String,
        gender: VoiceGender,
    ) = CloudVoice(name = name, languageCode = "pl-PL", gender = gender)

    private val polish = listOf(
        voice("pl-PL-Wavenet-A", VoiceGender.FEMALE),
        voice("pl-PL-Wavenet-B", VoiceGender.MALE),
        voice("pl-PL-Wavenet-C", VoiceGender.MALE),
        voice("pl-PL-Wavenet-D", VoiceGender.FEMALE),
        voice("pl-PL-Wavenet-E", VoiceGender.FEMALE),
        voice("pl-PL-Standard-A", VoiceGender.FEMALE),
        voice("pl-PL-Standard-B", VoiceGender.MALE),
    )

    /** The complaint that started all this: a woman's voice called Piotr. */
    @Test
    fun `a woman's voice never gets a man's name`() {
        nameVoices(polish)
            .filter { it.gender == VoiceGender.FEMALE }
            .forEach { assertTrue("${it.displayName} is not a woman's name", it.displayName in FEMALE_NAMES) }
    }

    @Test
    fun `a man's voice never gets a woman's name`() {
        nameVoices(polish)
            .filter { it.gender == VoiceGender.MALE }
            .forEach { assertTrue("${it.displayName} is not a man's name", it.displayName in MALE_NAMES) }
    }

    /**
     * An anchor the shared lists cannot fake: these are the exact names that landed on the
     * wrong voices when the device synthesiser was naming them by position.
     */
    @Test
    fun `the names from the bug report sit on the side they belong to`() {
        assertTrue("Zofia" in FEMALE_NAMES)
        assertTrue("Hanna" in FEMALE_NAMES)
        assertTrue("Alicja" in FEMALE_NAMES)
        assertTrue("Piotr" in MALE_NAMES)
        assertTrue("Marek" in MALE_NAMES)
        assertTrue((FEMALE_NAMES.toSet() intersect MALE_NAMES.toSet()).isEmpty())
    }

    @Test
    fun `there are more names than Google offers Polish voices`() {
        // 16 female and 18 male as of writing; short lists would leak raw voice ids.
        assertTrue(FEMALE_NAMES.size >= 16)
        assertTrue(MALE_NAMES.size >= 18)
    }

    @Test
    fun `no two voices share a name`() {
        val named = nameVoices(polish)

        assertEquals(named.size, named.map { it.displayName }.distinct().size)
    }

    @Test
    fun `every voice keeps its name whatever order they arrive in`() {
        val first = nameVoices(polish).associate { it.id to it.displayName }
        val second = nameVoices(polish.reversed()).associate { it.id to it.displayName }

        assertEquals(first, second)
    }

    @Test
    fun `the engine's own name is used once the given names run out`() {
        val many = (1..40).map { voice("pl-PL-Wavenet-$it", VoiceGender.FEMALE) }

        val named = nameVoices(many)

        assertEquals(many.size, named.size)
        assertEquals(many.size, named.map { it.displayName }.distinct().size)
    }

    @Test
    fun `the voice id is carried through so a choice can be stored`() {
        assertEquals(polish.map { it.name }.toSet(), nameVoices(polish).map { it.id }.toSet())
    }

    @Test
    fun `no voices means no names`() {
        assertTrue(nameVoices(emptyList()).isEmpty())
    }
}
