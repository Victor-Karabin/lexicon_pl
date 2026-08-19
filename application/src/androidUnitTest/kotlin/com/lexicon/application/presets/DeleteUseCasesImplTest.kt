package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteUseCasesImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)
    private val presetRepository: VocabularyPresetRepository = mockk(relaxed = true)

    @Test
    fun `deleting a word removes exactly that word`() =
        runTest {
            DeleteWordUseCaseImpl(vocabularyRepository)(VocabularyId(7L))

            coVerify(exactly = 1) { vocabularyRepository.deleteWord(7L) }
        }

    @Test
    fun `restoring a word puts back exactly that word`() =
        runTest {
            RestoreWordUseCaseImpl(vocabularyRepository)(VocabularyId(7L))

            coVerify(exactly = 1) { vocabularyRepository.restoreWord(7L) }
        }

    @Test
    fun `deleting a preset leaves the words it listed alone`() =
        runTest {
            DeletePresetUseCaseImpl(presetRepository)(PresetId("food"))

            coVerify(exactly = 1) { presetRepository.deletePreset("food") }
            coVerify(exactly = 0) { vocabularyRepository.deleteWord(any()) }
        }

    @Test
    fun `restoring a preset asks the catalogue to bring it back`() =
        runTest {
            RestorePresetUseCaseImpl(presetRepository)(PresetId("food"))

            coVerify(exactly = 1) { presetRepository.restorePreset("food") }
        }
}
