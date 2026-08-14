package com.lexicon.data.remote.translate

import com.lexicon.boundary.TranslationDirection
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * MyMemory is a translation memory, not an engine: when it has no good match it
 * answers with whatever segment came closest, which is not always a translation and
 * is sometimes not even a word. These cover what gets past that.
 */
class MyMemoryTranslatorTest {
    private val api: MyMemoryApi = mockk()
    private val translator = MyMemoryTranslator(api)

    private fun answers(text: String?) {
        coEvery { api.translate(any(), any()) } returns MyMemoryResponse(MyMemoryData(text))
    }

    private suspend fun translate(word: String = "dragon") = translator.translate(word, TranslationDirection.EN_TO_PL)

    @Test
    fun `a plain translation comes back trimmed`() =
        runTest {
            answers("  smok  ")
            assertEquals("smok", translate())
        }

    @Test
    fun `the query echoed back is not a translation`() =
        runTest {
            answers("dragon")
            assertNull(translate())
            // Casing differs but it is still the same word.
            answers("Dragon")
            assertNull(translate())
        }

    @Test
    fun `a stray sentence out of the memory is refused`() =
        runTest {
            // A real answer this API gave for "smok".
            answers("Iain Walker wrote:")
            assertNull(translate("smok"))
        }

    @Test
    fun `a phrase may still translate to a phrase`() =
        runTest {
            answers("dzień dobry")
            assertEquals("dzień dobry", translator.translate("good morning", TranslationDirection.EN_TO_PL))
        }

    @Test
    fun `an empty or absent answer is no answer`() =
        runTest {
            answers("   ")
            assertNull(translate())
            answers(null)
            assertNull(translate())
            coEvery { api.translate(any(), any()) } returns MyMemoryResponse(null)
            assertNull(translate())
        }

    @Test
    fun `a network failure is no answer rather than a crash`() =
        runTest {
            coEvery { api.translate(any(), any()) } throws java.io.IOException("offline")
            assertNull(translate())
        }

    @Test
    fun `the language pair follows the direction asked for`() =
        runTest {
            answers("smok")
            translator.translate("dragon", TranslationDirection.EN_TO_PL)
            io.mockk.coVerify { api.translate("dragon", "en|pl") }

            answers("dragon")
            translator.translate("smok", TranslationDirection.PL_TO_EN)
            io.mockk.coVerify { api.translate("smok", "pl|en") }
        }
}
