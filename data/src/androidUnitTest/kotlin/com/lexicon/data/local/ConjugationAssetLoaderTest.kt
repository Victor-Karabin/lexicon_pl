package com.lexicon.data.local

import com.lexicon.boundary.VerbConjugationBoundary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private const val SAMPLE = """
[
  {"bezokolicznik":"chodzić","translation":"to walk","ja":"chodzę","ty":"chodzisz","on/ona/ono":"chodzi",
   "my":"chodzimy","wy":"chodzicie","oni/one":"chodzą"},
  {"bezokolicznik":"być","ja":"jestem","ty":"jesteś","on/ona/ono":"jest",
   "my":"jesteśmy","wy":"jesteście","oni/one":"są"},
  {"bezokolicznik":"bać się","ja":"boję się","ty":"boisz się","on/ona/ono":"boi się",
   "my":"boimy się","wy":"boicie się","oni/one":"boją się"},
  {"bezokolicznik":"bajać","ja":"baję; bajam","ty":"bajesz; bajasz","on/ona/ono":"baje; baja",
   "my":"bajemy; bajamy","wy":"bajecie; bajacie","oni/one":"bają; bajają"},
  {"bezokolicznik":"boleć","ja":"N/A","ty":"N/A","on/ona/ono":"boli",
   "my":"N/A","wy":"N/A","oni/one":"bolą"},
  {"bezokolicznik":"beknąć","translation":"","ja":"","ty":"","on/ona/ono":"","my":"","wy":"","oni/one":""},
  {"bezokolicznik":"","ja":"x","ty":"","on/ona/ono":"","my":"","wy":"","oni/one":""}
]
"""

private val PERSON_KEYS = setOf(PERSON_JA, PERSON_TY, PERSON_ON, PERSON_MY, PERSON_WY, PERSON_ONI)

class ConjugationAssetLoaderTest {
    private val verbs: List<VerbConjugationBoundary> = parseConjugations(SAMPLE)

    private fun of(infinitive: String) = verbs.first { it.infinitive == infinitive }

    @Test
    fun `a fully conjugated verb keeps all six persons`() {
        assertEquals(6, of("chodzić").forms.size)
        assertEquals(listOf("chodzę"), of("chodzić").forms[PERSON_JA])
        assertEquals(listOf("chodzą"), of("chodzić").forms[PERSON_ONI])
    }

    @Test
    fun `Polish characters survive parsing`() {
        assertEquals(listOf("jesteśmy"), of("być").forms[PERSON_MY])
        assertEquals(listOf("są"), of("być").forms[PERSON_ONI])
    }

    @Test
    fun `a reflexive verb keeps sie attached to its form`() {
        assertEquals(listOf("boję się"), of("bać się").forms[PERSON_JA])
        assertEquals("bać się", of("bać się").infinitive)
    }

    @Test
    fun `alternatives separated by a semicolon become separate answers`() {
        assertEquals(listOf("baję", "bajam"), of("bajać").forms[PERSON_JA])
        assertEquals(listOf("bają", "bajają"), of("bajać").forms[PERSON_ONI])
    }

    @Test
    fun `N A marks a person as having no usable form`() {
        val bolec = of("boleć")

        assertNull(bolec.forms[PERSON_JA])
        assertNull(bolec.forms[PERSON_MY])
        assertEquals(listOf("boli"), bolec.forms[PERSON_ON])
        assertEquals(listOf("bolą"), bolec.forms[PERSON_ONI])
    }

    @Test
    fun `a verb with nothing filled in survives parsing with no forms`() {
        assertTrue(of("beknąć").forms.isEmpty())
    }

    @Test
    fun `an entry with no infinitive is dropped`() {
        assertTrue(verbs.none { it.infinitive.isBlank() })
    }

    /** The real 4,545-entry file, so a change to it cannot quietly break the course. */
    @Test
    fun `a translation is carried through when the source has one`() {
        assertEquals("to walk", of("chodzić").translation)
    }

    @Test
    fun `a blank translation becomes no translation rather than an empty string`() {
        assertNull(of("beknąć").translation)
    }

    @Test
    fun `the shipped asset parses cleanly`() {
        val shipped = File("src/androidMain/assets/conjugations.json")
        assertTrue("asset is missing", shipped.isFile)

        val parsed = parseConjugations(shipped.readText())

        assertTrue(parsed.size > 4_000)
        assertTrue(parsed.none { it.infinitive.isBlank() })
        assertTrue(parsed.all { entry -> entry.forms.values.all { forms -> forms.none(String::isBlank) } })
        assertTrue(parsed.all { entry -> entry.forms.keys.all { it in PERSON_KEYS } })
        assertTrue(parsed.any { it.forms.isEmpty() })
        assertTrue(parsed.any { it.infinitive.endsWith(" się") })
        assertTrue("every verb should carry a translation", parsed.all { !it.translation.isNullOrBlank() })
    }
}
