package com.lexicon.presentation.presets

import androidx.lifecycle.SavedStateHandle
import com.lexicon.interactors.presets.CreateWordUseCase
import com.lexicon.interactors.presets.GenerateWordExampleUseCase
import com.lexicon.interactors.presets.GetPinnedImageUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.GetWordUseCase
import com.lexicon.interactors.presets.SearchImageCandidatesUseCase
import com.lexicon.interactors.presets.SetWordPresetUseCase
import com.lexicon.interactors.presets.SuggestTranslationsUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.UpdateWordUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordVariantsTest {
    private val dispatcher = StandardTestDispatcher()

    private val suggestTranslations: SuggestTranslationsUseCase = mockk {
        coEvery { this@mockk(any(), any()) } returns persistentListOf()
        coEvery { this@mockk("water", toPolish = true) } returns persistentListOf("woda", "wódka")
        coEvery { this@mockk("woda", toPolish = false) } returns persistentListOf("water", "aqua")
    }
    private val translateWord: TranslateWordUseCase = mockk {
        coEvery { this@mockk(any(), any()) } returns null
    }
    private val searchImageCandidates: SearchImageCandidatesUseCase = mockk {
        coEvery { this@mockk(any(), any()) } returns persistentListOf()
    }
    private val getPresets: GetVocabularyPresetsUseCase = mockk {
        coEvery { this@mockk() } returns persistentListOf()
    }

    private fun viewModel() =
        CreateWordViewModel(
            savedStateHandle = SavedStateHandle(),
            createWord = mockk<CreateWordUseCase>(relaxed = true),
            updateWord = mockk<UpdateWordUseCase>(relaxed = true),
            getWord = mockk<GetWordUseCase>(relaxed = true),
            translateWord = translateWord,
            suggestTranslations = suggestTranslations,
            searchImageCandidates = searchImageCandidates,
            getPresets = getPresets,
            getWordPresetMemberships = mockk<GetWordPresetMembershipsUseCase>(relaxed = true),
            getPinnedImage = mockk<GetPinnedImageUseCase>(relaxed = true),
            generateExample = mockk<GenerateWordExampleUseCase>(relaxed = true),
            setWordPreset = mockk<SetWordPresetUseCase>(relaxed = true),
            speechSynthesizer = mockk(relaxed = true),
        )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `typing English offers Polish variants once the typing settles`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onTranslationChanged("water")
            assertEquals(persistentListOf<String>(), viewModel.uiState.value.textVariants)

            advanceUntilIdle()

            assertEquals(listOf("woda", "wódka"), viewModel.uiState.value.textVariants)
        }

    @Test
    fun `typing Polish offers English variants`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onTextChanged("woda")
            advanceUntilIdle()

            assertEquals(listOf("water", "aqua"), viewModel.uiState.value.translationVariants)
        }

    @Test
    fun `choosing a variant fills the field it was offered for`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onTranslationChanged("water")
            advanceUntilIdle()
            viewModel.onVariantChosen("wódka", forPolish = true)

            assertEquals("wódka", viewModel.uiState.value.text)
            assertEquals("water", viewModel.uiState.value.translation)
        }

    @Test
    fun `variants are offered even when the other field is already filled`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onTextChanged("dom")
            advanceUntilIdle()
            viewModel.onTranslationChanged("water")
            advanceUntilIdle()

            assertEquals(listOf("woda", "wódka"), viewModel.uiState.value.textVariants)
            assertEquals("dom", viewModel.uiState.value.text)
        }

    @Test
    fun `typing into a field clears the variants that were offered for the other one`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onTextChanged("woda")
            advanceUntilIdle()
            viewModel.onTextChanged("wod")

            assertEquals(persistentListOf<String>(), viewModel.uiState.value.translationVariants)
        }
}
