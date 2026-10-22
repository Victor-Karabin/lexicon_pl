package com.lexicon.data.local

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AssetFingerprintTest {
    private fun reader(text: String): AssetReader = mockk { every { readText(any()) } returns text }

    private fun fingerprints(text: String): Map<String, String> {
        val assets = reader(text)
        return mapOf(
            "vocabulary" to VocabularySeedAssetLoader(assets).fingerprint(),
            "presets" to VocabularyPresetAssetLoader(assets).fingerprint(),
            "course" to CourseAssetLoader(assets).fingerprint(),
            "conjugations" to ConjugationAssetLoader(assets).fingerprint(),
        )
    }

    @Test
    fun `every loader fingerprints the same text the same way`() {
        val all = fingerprints("[{\"a\":1}]")

        assertEquals(all.toString(), 1, all.values.toSet().size)
    }

    @Test
    fun `a changed asset of the same length gets a different fingerprint in every loader`() {
        val before = fingerprints("[{\"bezokolicznik\":\"mam\"}]")
        val after = fingerprints("[{\"bezokolicznik\":\"maM\"}]")

        before.forEach { (name, fingerprint) -> assertNotEquals(name, fingerprint, after.getValue(name)) }
    }

    @Test
    fun `a fingerprint is made of the text's length and hash, not of template syntax`() {
        fingerprints("[]").forEach { (name, fingerprint) ->
            assertFalse("$name: $fingerprint", fingerprint.contains('$'))
            assertFalse("$name: $fingerprint", fingerprint.contains('{'))
        }
    }
}
