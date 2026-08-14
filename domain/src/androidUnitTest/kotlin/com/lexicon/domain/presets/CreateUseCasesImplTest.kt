package com.lexicon.domain.presets

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.PresetDraftException
import com.lexicon.interactors.presets.PresetDraftProblem
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.WordDraftException
import com.lexicon.interactors.presets.WordDraftProblem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateUseCasesImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)
    private val presetRepository: VocabularyPresetRepository = mockk(relaxed = true)
    private val imageProvider: ImageProvider = mockk(relaxed = true)

    private val createWord = CreateWordUseCaseImpl(vocabularyRepository, presetRepository, imageProvider)

    private val stored = VocabularyItemBoundary(
        id = -1,
        text = "smok",
        translation = "dragon",
        transcription = "",
        isFavourite = false,
        cefr = null,
    )

    private fun wordExists(exists: Boolean) {
        coEvery { vocabularyRepository.findWordByText(any()) } returns if (exists) stored else null
        coEvery { vocabularyRepository.createWord(any(), any(), any()) } returns stored
    }

    @Test
    fun `a word is stored, filed under the chosen presets, and its picture pinned`() =
        runTest {
            wordExists(false)

            val result = createWord(
                text = "  smok  ",
                translation = "  dragon  ",
                imageUrl = "https://example.com/dragon.jpg",
                presetIds = listOf(PresetId("fantasy"), PresetId("fantasy"), PresetId("animals")),
            )

            assertTrue(result.isSuccess)
            // Trimmed on the way in, so a stray space does not become part of the word.
            coVerify { vocabularyRepository.createWord("smok", "dragon", "") }
            // Duplicates in the chip selection must not double-insert.
            coVerify(exactly = 1) { presetRepository.setWordInPreset("fantasy", -1, true) }
            coVerify(exactly = 1) { presetRepository.setWordInPreset("animals", -1, true) }
            // Pinned against the English side, which is the key the trainings search by.
            coVerify { imageProvider.pinImage("dragon", "https://example.com/dragon.jpg") }
        }

    @Test
    fun `a word already in the vocabulary is refused rather than duplicated`() =
        runTest {
            wordExists(true)

            val result = createWord(text = "smok", translation = "dragon")

            assertEquals(WordDraftProblem.ALREADY_EXISTS, (result.exceptionOrNull() as WordDraftException).problem)
            coVerify(exactly = 0) { vocabularyRepository.createWord(any(), any(), any()) }
        }

    @Test
    fun `a word with only spaces in a field is refused`() =
        runTest {
            wordExists(false)

            assertEquals(
                WordDraftProblem.MISSING_TEXT,
                (createWord(text = "   ", translation = "dragon").exceptionOrNull() as WordDraftException).problem,
            )
            assertEquals(
                WordDraftProblem.MISSING_TRANSLATION,
                (createWord(text = "smok", translation = " ").exceptionOrNull() as WordDraftException).problem,
            )
        }

    @Test
    fun `no picture is pinned when none was chosen`() =
        runTest {
            wordExists(false)

            createWord(text = "smok", translation = "dragon", imageUrl = null)

            coVerify(exactly = 0) { imageProvider.pinImage(any(), any()) }
        }

    @Test
    fun `a preset without a name is refused`() =
        runTest {
            val result = CreatePresetUseCaseImpl(presetRepository)(title = "   ")

            assertEquals(
                PresetDraftProblem.MISSING_TITLE,
                (result.exceptionOrNull() as PresetDraftException).problem,
            )
            coVerify(exactly = 0) { presetRepository.createPreset(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `a preset with no description is stored without one rather than with an empty string`() =
        runTest {
            coEvery { presetRepository.createPreset(any(), any(), any(), any(), any()) } returns
                VocabularyPresetBoundary(
                    id = "my-kitchen",
                    categoryId = "my-presets",
                    title = mapOf("en" to "Kitchen"),
                    description = emptyMap(),
                    icon = "restaurant",
                    color = "#2E7D32",
                    popularity = 0,
                    estimatedSeconds = 0,
                    vocabularyIds = emptyList(),
                )
            coEvery { presetRepository.getCategories() } returns
                listOf(PresetCategoryBoundary("my-presets", -1, mapOf("en" to "My presets")))

            val result = CreatePresetUseCaseImpl(presetRepository)(title = "Kitchen", description = "  ")

            assertTrue(result.isSuccess)
            coVerify {
                presetRepository.createPreset(
                    mapOf("en" to "Kitchen"),
                    emptyMap(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `translation runs in the direction the caller asked for`() =
        runTest {
            val translator: Translator = mockk()
            coEvery { translator.translate("water", TranslationDirection.EN_TO_PL) } returns "  woda  "
            coEvery { translator.translate("woda", TranslationDirection.PL_TO_EN) } returns "water"
            val translate = TranslateWordUseCaseImpl(translator)

            assertEquals("woda", translate("water", toPolish = true))
            assertEquals("water", translate("woda", toPolish = false))
        }

    @Test
    fun `nothing to translate and nothing found both come back as no suggestion`() =
        runTest {
            val translator: Translator = mockk()
            coEvery { translator.translate(any(), any()) } returns null
            val translate = TranslateWordUseCaseImpl(translator)

            assertNull(translate("   ", toPolish = true))
            assertNull(translate("gibberish", toPolish = true))
        }
}
