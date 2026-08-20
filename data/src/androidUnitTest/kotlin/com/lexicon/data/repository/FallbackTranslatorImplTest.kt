package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FallbackTranslatorImplTest {
    private val corpus: Translator = mockk()
    private val remote: Translator = mockk()

    private val translator = FallbackTranslatorImpl(listOf(corpus, remote))

    @Test
    fun `the corpus answers without the remote service being troubled`() =
        runTest {
            coEvery { corpus.translate("water", any()) } returns "woda"

            assertEquals("woda", translator.translate("water", TranslationDirection.EN_TO_PL))
            coVerify(exactly = 0) { remote.translate(any(), any()) }
        }

    @Test
    fun `a word the corpus does not have falls through to the remote service`() =
        runTest {
            coEvery { corpus.translate(any(), any()) } returns null
            coEvery { remote.translate("dragon", any()) } returns "smok"

            assertEquals("smok", translator.translate("dragon", TranslationDirection.EN_TO_PL))
        }

    @Test
    fun `a blank answer is treated as no answer and the chain carries on`() =
        runTest {
            coEvery { corpus.translate(any(), any()) } returns "   "
            coEvery { remote.translate(any(), any()) } returns "smok"

            assertEquals("smok", translator.translate("dragon", TranslationDirection.EN_TO_PL))
        }

    @Test
    fun `nothing anywhere is no suggestion rather than a failure`() =
        runTest {
            coEvery { corpus.translate(any(), any()) } returns null
            coEvery { remote.translate(any(), any()) } returns null

            assertNull(translator.translate("qqqq", TranslationDirection.EN_TO_PL))
        }

    @Test
    fun `a chain with no remote translator still works offline`() =
        runTest {
            coEvery { corpus.translate("water", any()) } returns "woda"

            val offline = FallbackTranslatorImpl(listOf(corpus))

            assertEquals("woda", offline.translate("water", TranslationDirection.EN_TO_PL))
        }

    @Test
    fun `blank input asks nobody`() =
        runTest {
            assertNull(translator.translate("   ", TranslationDirection.EN_TO_PL))
            coVerify(exactly = 0) { corpus.translate(any(), any()) }
        }
}
