package com.lexicon.presentation.presets

import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.ObserveVocabularyPresetsUseCase
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.SetPresetFavouriteUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyViewModelDeleteTest {
    private val dispatcher = StandardTestDispatcher()

    private val kot = PresetWord(VocabularyId(1L), "kot", "cat", "kɔt")
    private val pies = PresetWord(VocabularyId(2L), "pies", "dog", "pjɛs")

    private val searchVocabulary: SearchVocabularyUseCase = mockk {
        coEvery { this@mockk(any(), any(), any()) } returns persistentListOf(kot, pies)
    }
    private val deleteWord: DeleteWordUseCase = mockk(relaxed = true)
    private val restoreWord: RestoreWordUseCase = mockk(relaxed = true)
    private val presets = MutableStateFlow(persistentListOf<VocabularyPreset>())
    private val observePresets: ObserveVocabularyPresetsUseCase = mockk {
        every { this@mockk() } returns presets
    }
    private val observeFavourites: ObserveFavouriteWordIdsUseCase = mockk {
        every { this@mockk() } returns flowOf(emptySet())
    }

    private fun viewModel() =
        VocabularyViewModel(
            observePresets = observePresets,
            searchVocabulary = searchVocabulary,
            setPresetFavourite = mockk<SetPresetFavouriteUseCase>(relaxed = true),
            toggleWordFavourite = mockk<ToggleWordFavouriteUseCase>(relaxed = true),
            deleteWord = deleteWord,
            restoreWord = restoreWord,
            deletePreset = mockk<DeletePresetUseCase>(relaxed = true),
            restorePreset = mockk<RestorePresetUseCase>(relaxed = true),
            observeFavouriteWordIds = observeFavourites,
            dispatchers = object : DispatcherProvider {
                override val io: CoroutineDispatcher get() = dispatcher
                override val default: CoroutineDispatcher get() = dispatcher
                override val main: CoroutineDispatcher get() = dispatcher
            },
        )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun loaded(state: VocabularyUiState) = state as VocabularyUiState.Loaded

    private fun preset(vararg wordIds: Long) =
        VocabularyPreset(
            id = PresetId("food"),
            title = LocalizedText(mapOf("en" to "Food")),
            description = LocalizedText(mapOf("en" to "")),
            category = PresetCategory("everyday-life", 3, LocalizedText(mapOf("en" to "Everyday life"))),
            icon = null,
            color = null,
            popularity = 1,
            estimatedDuration = 5.minutes,
            vocabularyIds = wordIds.map(::VocabularyId).toImmutableList(),
        )

    /**
     * Regression: the browser fetched its presets once, so a word deleted on the detail screen
     * left it showing a preset that still counted the word — its heart could never read as full
     * again. The list is observed now, so a change made anywhere reaches it.
     */
    @Test
    fun `the preset list reflects a word deleted somewhere else`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            presets.value = persistentListOf(preset(1L, 2L))
            advanceUntilIdle()

            presets.value = persistentListOf(preset(2L))
            advanceUntilIdle()

            val listed = (viewModel.uiState.value as VocabularyUiState.Loaded).presets.single()
            assertEquals(listOf(VocabularyId(2L)), listed.vocabularyIds)
        }

    @Test
    fun `deleting a word asks for it to be deleted`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteWord(VocabularyId(1L)) }
        }

    /** The row has to go at once; waiting for a re-query would leave it under the finger. */
    @Test
    fun `a deleted word leaves the list immediately`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onQueryChanged("k")
            advanceUntilIdle()

            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            assertEquals(listOf("pies"), loaded(viewModel.uiState.value).words.map { it.text })
        }

    @Test
    fun `a deletion is remembered so it can be undone`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onQueryChanged("k")
            advanceUntilIdle()

            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            val deleted = loaded(viewModel.uiState.value).lastDeleted
            assertTrue(deleted is DeletedItem.Word)
            assertEquals("kot", deleted?.label)
        }

    @Test
    fun `undoing a deletion restores the word and re-runs the search`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onQueryChanged("k")
            advanceUntilIdle()
            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            viewModel.onUndoDelete()
            advanceUntilIdle()

            coVerify(exactly = 1) { restoreWord(VocabularyId(1L)) }
            assertEquals(listOf("kot", "pies"), loaded(viewModel.uiState.value).words.map { it.text })
        }

    /** Cleared once shown, or the same undo would be offered again on the next state change. */
    @Test
    fun `the undo is cleared once its message has been shown`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onQueryChanged("k")
            advanceUntilIdle()
            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            viewModel.onDeleteMessageShown()
            advanceUntilIdle()

            assertNull(loaded(viewModel.uiState.value).lastDeleted)
        }

    @Test
    fun `deleting one word leaves the others alone`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onQueryChanged("k")
            advanceUntilIdle()

            viewModel.onWordDeleted(kot)
            advanceUntilIdle()

            coVerify(exactly = 0) { deleteWord(VocabularyId(2L)) }
            assertEquals(
                listOf(pies).toImmutableList(),
                loaded(viewModel.uiState.value).words,
            )
        }
}
