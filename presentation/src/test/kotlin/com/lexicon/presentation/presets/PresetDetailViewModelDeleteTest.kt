package com.lexicon.presentation.presets

import androidx.lifecycle.SavedStateHandle
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetUseCase
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.ObserveStudySetIdsUseCase
import com.lexicon.interactors.presets.PresetStudySetState
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SetPresetInStudySetUseCase
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.ToggleWordInStudySetUseCase
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.PresetCategory
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.VocabularyPreset
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class PresetDetailViewModelDeleteTest {
    private val dispatcher = StandardTestDispatcher()

    private val kot = Word(VocabularyId(1L), "kot", "cat", "kɔt")
    private val pies = Word(VocabularyId(2L), "pies", "dog", "pjɛs")

    private var storedWords = listOf(kot, pies)
    private val studySet = MutableStateFlow<Set<VocabularyId>>(emptySet())

    private fun presetOf(words: List<Word>) =
        VocabularyPreset(
            id = PresetId("food"),
            title = LocalizedText(mapOf("en" to "Food")),
            description = LocalizedText(mapOf("en" to "")),
            category = PresetCategory("everyday-life", 3, LocalizedText(mapOf("en" to "Everyday life"))),
            icon = null,
            color = null,
            popularity = 1,
            estimatedDuration = 5.minutes,
            vocabularyIds = words.map { it.id }.toImmutableList(),
        )

    private val getPreset: GetVocabularyPresetUseCase = mockk {
        coEvery { this@mockk(any()) } answers { presetOf(storedWords) }
    }
    private val getPresetVocabulary: GetPresetVocabularyUseCase = mockk {
        coEvery { this@mockk(any()) } answers { storedWords.toImmutableList() }
    }

    private val deleteWord = object : DeleteWordUseCase {
        override suspend fun invoke(id: VocabularyId) {
            storedWords = storedWords.filterNot { it.id == id }
        }
    }

    private val restoreWord = object : RestoreWordUseCase {
        override suspend fun invoke(id: VocabularyId) {
            storedWords = listOf(kot, pies)
        }
    }

    private val speechSynthesizer: SpeechSynthesizer = mockk(relaxed = true)

    private val setWordPresetMembership = object : SetWordPresetMembershipUseCase {
        override suspend fun invoke(
            presetId: PresetId,
            wordId: VocabularyId,
            isMember: Boolean,
        ) {
            storedWords = if (isMember) storedWords else storedWords.filterNot { it.id == wordId }
        }
    }

    private fun viewModel() =
        PresetDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PRESET_ID_ARG to "food")),
            getPreset = getPreset,
            getPresetVocabulary = getPresetVocabulary,
            toggleWordInStudySet = mockk<ToggleWordInStudySetUseCase>(relaxed = true),
            deleteWord = deleteWord,
            restoreWord = restoreWord,
            setPresetInStudySet = mockk<SetPresetInStudySetUseCase>(relaxed = true),
            observeStudySetIds = mockk<ObserveStudySetIdsUseCase> { every { this@mockk() } returns studySet },
            getWordPresetMemberships = mockk<GetWordPresetMembershipsUseCase>(relaxed = true),
            setWordPresetMembership = setWordPresetMembership,
            dispatchers = object : DispatcherProvider {
                override val io: CoroutineDispatcher get() = dispatcher
                override val default: CoroutineDispatcher get() = dispatcher
                override val main: CoroutineDispatcher get() = dispatcher
            },
            speechSynthesizer = speechSynthesizer,
        )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        storedWords = listOf(kot, pies)
        studySet.value = emptySet()
    }

    private fun TestScope.started(): PresetDetailViewModel =
        viewModel().also { backgroundScope.launch(dispatcher) { it.uiState.collect { } } }

    private fun loaded(viewModel: PresetDetailViewModel) = viewModel.uiState.value as PresetDetailUiState.Loaded

    @Test
    fun `pronouncing a word speaks the Polish, not the translation`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onPronounceWord(Word(VocabularyId(1), "chleb", "bread", "xlɛp"))
            advanceUntilIdle()

            coVerify { speechSynthesizer.speak("chleb") }
            coVerify(exactly = 0) { speechSynthesizer.speak("bread") }
        }

    @Test
    fun `a word unticked from this preset leaves the list, without waiting for a reopen`() =
        runTest(dispatcher) {
            val viewModel = started()
            advanceUntilIdle()

            viewModel.onChangePresetsRequested(kot)
            advanceUntilIdle()
            viewModel.onPresetMembershipToggled(PresetId("food"), isMember = false)
            advanceUntilIdle()

            assertEquals(listOf("pies"), loaded(viewModel).words.map { it.text })
        }

    @Test
    fun `the header count follows a word being untangled from the preset`() =
        runTest(dispatcher) {
            val viewModel = started()
            advanceUntilIdle()

            viewModel.onChangePresetsRequested(kot)
            advanceUntilIdle()
            viewModel.onPresetMembershipToggled(PresetId("food"), isMember = false)
            advanceUntilIdle()

            assertEquals(1, loaded(viewModel).preset.vocabularyIds.size)
        }

    @Test
    fun `a deleted word leaves the list`() =
        runTest(dispatcher) {
            val viewModel = started()
            advanceUntilIdle()

            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            assertEquals(listOf("pies"), loaded(viewModel).words.map { it.text })
        }

    @Test
    fun `the preset stops counting a word that was deleted`() =
        runTest(dispatcher) {
            val viewModel = started()
            advanceUntilIdle()

            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            assertEquals(listOf(VocabularyId(2L)), loaded(viewModel).preset.vocabularyIds)
        }

    @Test
    fun `starring what is left reads as fully in the study set`() =
        runTest(dispatcher) {
            val viewModel = started()
            advanceUntilIdle()
            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            studySet.value = setOf(VocabularyId(2L))
            advanceUntilIdle()

            assertEquals(PresetStudySetState.ALL, loaded(viewModel).studySetState)
        }

    @Test
    fun `undoing a deletion puts the word back into the preset`() =
        runTest(dispatcher) {
            val viewModel = started()
            advanceUntilIdle()
            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            viewModel.onUndoDelete()
            advanceUntilIdle()

            assertEquals(
                listOf(VocabularyId(1L), VocabularyId(2L)),
                loaded(viewModel).preset.vocabularyIds,
            )
            assertEquals(persistentListOf("kot", "pies"), loaded(viewModel).words.map { it.text }.toImmutableList())
        }

    @Test
    fun `deleting the last word leaves an empty list rather than a spinner`() =
        runTest(dispatcher) {
            val viewModel = started()
            advanceUntilIdle()

            viewModel.onWordDeleted(kot)
            viewModel.onWordDeleted(pies)
            advanceUntilIdle()

            assertEquals(emptyList<String>(), loaded(viewModel).words.map { it.text })
            assertEquals(false, loaded(viewModel).isLoadingWords)
        }
}
