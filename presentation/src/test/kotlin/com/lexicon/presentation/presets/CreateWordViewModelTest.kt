package com.lexicon.presentation.presets

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.lexicon.interactors.presets.CreateWordUseCase
import com.lexicon.interactors.presets.GetPinnedImageUseCase
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.GetWordUseCase
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.SuggestTranslationsUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.WordDraftException
import com.lexicon.interactors.presets.WordDraftProblem
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.VocabularyPreset
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.ImmutableList
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

    private val getWord: GetWordUseCase = mockk()
    private val getWordPresetMemberships: GetWordPresetMembershipsUseCase = mockk()
    private val getPinnedImage: GetPinnedImageUseCase = mockk { coEvery { this@mockk(any()) } returns null }

    private var presets: ImmutableList<VocabularyPreset> = persistentListOf()
    private val presetWrites = mutableListOf<PresetWrite>()
    private var presetWritesFail = false

    private val setWordPreset: SetWordPresetMembershipUseCase = mockk {
        coEvery { this@mockk(any(), any(), any()) } answers {
            presetWrites += PresetWrite(secondArg<VocabularyId>().value, firstArg<PresetId>().value, thirdArg())
            if (presetWritesFail) throw IllegalStateException("disk full")
        }
    }

    private var logged = 0

    private data class PresetWrite(
        val wordId: Long,
        val presetId: String,
        val isMember: Boolean,
    )

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

    private fun viewModel(wordId: Long? = null) =
        CreateWordViewModel(
            savedStateHandle = SavedStateHandle(wordId?.let { mapOf(WORD_ID_ARG to it.toString()) }.orEmpty()),
            createWord = createWord,
            updateWord = mockk(),
            getWord = getWord,
            translateWord = translateWord,
            suggestTranslations = suggestTranslations,
            searchImageCandidates = mockk { coEvery { this@mockk(any(), any()) } returns persistentListOf() },
            getPresets = mockk { coEvery { this@mockk() } answers { presets } },
            getWordPresetMemberships = getWordPresetMemberships,
            getPinnedImage = getPinnedImage,
            generateExample = mockk(),
            setWordPreset = setWordPreset,
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

    private fun givenExistingWord(vararg memberships: Pair<String, Boolean>) {
        coEvery { getWord(VocabularyId(WORD_ID)) } returns Word(VocabularyId(WORD_ID), "woda", "water", "x")
        coEvery { getWordPresetMemberships(VocabularyId(WORD_ID)) } returns membershipsOf(*memberships)
    }

    private fun CreateWordViewModel.isMember(presetId: String) =
        uiState.value.memberships.single { it.preset.id == PresetId(presetId) }.isMember

    @Test
    fun `switching a preset on for an existing word writes that word into that preset`() =
        runTest(dispatcher) {
            givenExistingWord("food" to false, "travel" to false)
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()

            viewModel.onPresetToggled(PresetId("food"), isMember = true)
            advanceUntilIdle()

            assertEquals(listOf(PresetWrite(WORD_ID, "food", true)), presetWrites)
            assertTrue(viewModel.isMember("food"))
            assertFalse(viewModel.isMember("travel"))
            assertFalse(viewModel.uiState.value.presetFailed)
        }

    @Test
    fun `switching a preset off for an existing word writes the removal`() =
        runTest(dispatcher) {
            givenExistingWord("food" to true, "travel" to true)
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()

            viewModel.onPresetToggled(PresetId("travel"), isMember = false)
            advanceUntilIdle()

            assertEquals(listOf(PresetWrite(WORD_ID, "travel", false)), presetWrites)
            assertTrue(viewModel.isMember("food"))
            assertFalse(viewModel.isMember("travel"))
        }

    @Test
    fun `the chip answers before the write has run`() =
        runTest(dispatcher) {
            givenExistingWord("food" to false)
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()

            viewModel.onPresetToggled(PresetId("food"), isMember = true)

            assertTrue(viewModel.isMember("food"))
            assertTrue(presetWrites.isEmpty())
        }

    @Test
    fun `a failed write puts the chip back and says the preset failed`() =
        runTest(dispatcher) {
            givenExistingWord("food" to false, "travel" to true)
            presetWritesFail = true
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()

            viewModel.onPresetToggled(PresetId("food"), isMember = true)
            advanceUntilIdle()

            assertEquals(listOf(PresetWrite(WORD_ID, "food", true)), presetWrites)
            assertFalse(viewModel.isMember("food"))
            assertTrue(viewModel.isMember("travel"))
            assertTrue(viewModel.uiState.value.presetFailed)
        }

    @Test
    fun `a failed removal puts the chip back on`() =
        runTest(dispatcher) {
            givenExistingWord("food" to true)
            presetWritesFail = true
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()

            viewModel.onPresetToggled(PresetId("food"), isMember = false)
            advanceUntilIdle()

            assertTrue(viewModel.isMember("food"))
            assertTrue(viewModel.uiState.value.presetFailed)
        }

    @Test
    fun `the preset failure goes away once shown`() =
        runTest(dispatcher) {
            givenExistingWord("food" to false)
            presetWritesFail = true
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()
            viewModel.onPresetToggled(PresetId("food"), isMember = true)
            advanceUntilIdle()

            viewModel.onErrorShown()

            assertFalse(viewModel.uiState.value.presetFailed)
        }

    @Test
    fun `the next toggle clears an earlier preset failure`() =
        runTest(dispatcher) {
            givenExistingWord("food" to false)
            presetWritesFail = true
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()
            viewModel.onPresetToggled(PresetId("food"), isMember = true)
            advanceUntilIdle()
            presetWritesFail = false

            viewModel.onPresetToggled(PresetId("food"), isMember = true)

            assertFalse(viewModel.uiState.value.presetFailed)
            advanceUntilIdle()
            assertTrue(viewModel.isMember("food"))
        }

    @Test
    fun `toggling the same preset twice quickly writes only the last choice`() =
        runTest(dispatcher) {
            givenExistingWord("food" to false)
            val viewModel = viewModel(WORD_ID)
            advanceUntilIdle()

            viewModel.onPresetToggled(PresetId("food"), isMember = true)
            viewModel.onPresetToggled(PresetId("food"), isMember = false)
            advanceUntilIdle()

            assertEquals(listOf(PresetWrite(WORD_ID, "food", false)), presetWrites)
            assertFalse(viewModel.isMember("food"))
        }

    @Test
    fun `for a new word the chips change but nothing is written until the save`() =
        runTest(dispatcher) {
            presets = persistentListOf(presetOf("food"), presetOf("travel"))
            val savedPresets = slot<List<PresetId>>()
            coEvery { createWord(any(), any(), any(), any(), capture(savedPresets)) } returns
                Result.success(Word(VocabularyId(1L), "woda", "water", "x"))
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onPresetToggled(PresetId("travel"), isMember = true)
            advanceUntilIdle()

            assertTrue(viewModel.isMember("travel"))
            assertFalse(viewModel.isMember("food"))
            assertTrue(presetWrites.isEmpty())
            assertFalse(viewModel.uiState.value.presetFailed)

            viewModel.onTextChanged("woda")
            viewModel.onTranslationChanged("water")
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(listOf(PresetId("travel")), savedPresets.captured)
            assertTrue(presetWrites.isEmpty())
        }

    private companion object {
        const val SETTLED_MS = 700L
        const val WORD_ID = 7L
    }
}
