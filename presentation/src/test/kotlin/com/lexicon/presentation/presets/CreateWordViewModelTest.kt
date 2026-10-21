package com.lexicon.presentation.presets

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.lexicon.interactors.presets.CreateWordUseCase
import com.lexicon.interactors.presets.SuggestTranslationsUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.WordDraftException
import com.lexicon.interactors.presets.WordDraftProblem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateWordViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val createWord: CreateWordUseCase = mockk()
    private val translateWord: TranslateWordUseCase = mockk()
    private val suggestTranslations: SuggestTranslationsUseCase = mockk {
        coEvery { this@mockk(any(), any()) } returns persistentListOf()
    }

    private var logged = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } answers { logged++ }
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } answers { logged++ }
        every { Log.w(any<String>(), any<Throwable>()) } answers { logged++ }
        every { Log.e(any<String>(), any<String>()) } answers { logged++ }
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } answers { logged++ }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        CreateWordViewModel(
            savedStateHandle = SavedStateHandle(),
            createWord = createWord,
            updateWord = mockk(),
            getWord = mockk(),
            translateWord = translateWord,
            suggestTranslations = suggestTranslations,
            searchImageCandidates = mockk { coEvery { this@mockk(any(), any()) } returns persistentListOf() },
            getPresets = mockk { coEvery { this@mockk() } returns persistentListOf() },
            getWordPresetMemberships = mockk(),
            getPinnedImage = mockk(),
            generateExample = mockk(),
            setWordPreset = mockk(),
            speechSynthesizer = mockk(relaxed = true),
        )

    @Test
    fun `clearing the field while a translation is under way stops the Translating indicator`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<String?>()
            coEvery { translateWord(any(), any()) } coAnswers { gate.await() }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onTextChanged("woda")
            advanceTimeBy(SETTLED_MS)
            assertTrue(viewModel.uiState.value.isTranslating)

            viewModel.onTextChanged("")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isTranslating)
        }

    @Test
    fun `choosing a variant while a translation is under way stops the Translating indicator`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<String?>()
            coEvery { translateWord(any(), any()) } coAnswers { gate.await() }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onTextChanged("woda")
            advanceTimeBy(SETTLED_MS)
            assertTrue(viewModel.uiState.value.isTranslating)

            viewModel.onVariantChosen("water", forPolish = false)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isTranslating)
        }

    @Test
    fun `a finished translation fills the field and stops the Translating indicator`() =
        runTest(dispatcher) {
            coEvery { translateWord("woda", false) } returns "water"
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onTextChanged("woda")
            advanceUntilIdle()

            assertEquals("water", viewModel.uiState.value.translation)
            assertFalse(viewModel.uiState.value.isTranslating)
        }

    @Test
    fun `a save that fails for any other reason is logged and shown as a failed save, not as missing text`() =
        runTest(dispatcher) {
            coEvery { createWord(any(), any(), any(), any(), any()) } returns Result.failure(IllegalStateException("disk full"))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTextChanged("woda")
            viewModel.onTranslationChanged("water")

            viewModel.onSave()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertNull(state.problem)
            assertTrue(state.saveFailed)
            assertTrue("the failure should be logged", logged > 0)
        }

    @Test
    fun `a save refused for a draft problem keeps that problem and is not a failed save`() =
        runTest(dispatcher) {
            coEvery { createWord(any(), any(), any(), any(), any()) } returns
                Result.failure(WordDraftException(WordDraftProblem.ALREADY_EXISTS))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTextChanged("woda")
            viewModel.onTranslationChanged("water")

            viewModel.onSave()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(WordDraftProblem.ALREADY_EXISTS, state.problem)
            assertFalse(state.saveFailed)
        }

    @Test
    fun `the failed save message goes away once shown`() =
        runTest(dispatcher) {
            coEvery { createWord(any(), any(), any(), any(), any()) } returns Result.failure(IllegalStateException("disk full"))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTextChanged("woda")
            viewModel.onTranslationChanged("water")
            viewModel.onSave()
            advanceUntilIdle()

            viewModel.onErrorShown()

            assertFalse(viewModel.uiState.value.saveFailed)
        }

    private companion object {
        const val SETTLED_MS = 700L
    }
}
