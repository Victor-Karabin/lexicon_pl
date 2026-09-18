package com.lexicon.presentation.presets

import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.ObserveVocabularyPresetsUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.VocabularyPreset
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyStatusTest {
    private val dispatcher = StandardTestDispatcher()

    private val kot = Word(VocabularyId(1L), "kot", "cat", "kɔt")

    private val searchVocabulary: SearchVocabularyUseCase = mockk {
        coEvery { this@mockk(any(), any(), any(), any(), any()) } returns persistentListOf(kot)
    }
    private val setWordStatus: SetWordStatusUseCase = mockk(relaxed = true)
    private val statuses = MutableStateFlow<Map<VocabularyId, WordStatus>>(emptyMap())
    private val observeStatuses: ObserveWordStatusesUseCase = mockk {
        every { this@mockk() } returns statuses
    }
    private val observePresets: ObserveVocabularyPresetsUseCase = mockk {
        every { this@mockk() } returns MutableStateFlow(persistentListOf<VocabularyPreset>())
    }

    private fun viewModel() =
        VocabularyViewModel(
            observePresets = observePresets,
            searchVocabulary = searchVocabulary,
            setWordStatus = setWordStatus,
            deleteWord = mockk<DeleteWordUseCase>(relaxed = true),
            restoreWord = mockk<RestoreWordUseCase>(relaxed = true),
            deletePreset = mockk<DeletePresetUseCase>(relaxed = true),
            restorePreset = mockk<RestorePresetUseCase>(relaxed = true),
            observeWordStatuses = observeStatuses,
            getWordPresetMemberships = mockk<GetWordPresetMembershipsUseCase>(relaxed = true),
            setWordPresetMembership = mockk<SetWordPresetMembershipUseCase>(relaxed = true),
            dispatchers = object : DispatcherProvider {
                override val io: CoroutineDispatcher get() = dispatcher
                override val default: CoroutineDispatcher get() = dispatcher
                override val main: CoroutineDispatcher get() = dispatcher
            },
            speechSynthesizer = mockk(relaxed = true),
        )

    private fun loaded(viewModel: VocabularyViewModel) = viewModel.uiState.value as VocabularyUiState.Loaded

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `tapping an unmarked word marks it to learn and the row shows it at once`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onWordStatusCycled(kot.id)

            assertEquals(WordStatus.TO_LEARN, loaded(viewModel).wordStatuses[kot.id])
            advanceUntilIdle()
            coVerify { setWordStatus(kot.id, WordStatus.TO_LEARN) }
        }

    @Test
    fun `tapping again walks to favourite, known and back to nothing`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            val seen = buildList {
                repeat(4) {
                    viewModel.onWordStatusCycled(kot.id)
                    add(loaded(viewModel).wordStatuses[kot.id])
                }
            }

            assertEquals(
                listOf(WordStatus.TO_LEARN, WordStatus.FAVOURITE, WordStatus.KNOWN, WordStatus.UNDEFINED),
                seen,
            )
        }

    @Test
    fun `a status written elsewhere is the one the next tap moves on from`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            statuses.value = mapOf(kot.id to WordStatus.KNOWN)
            advanceUntilIdle()

            viewModel.onWordStatusCycled(kot.id)
            advanceUntilIdle()

            coVerify { setWordStatus(kot.id, WordStatus.UNDEFINED) }
        }

    @Test
    fun `the to learn filter asks the search for the learning list alone`() =
        runTest(dispatcher) {
            val learningOnly = slot<Boolean>()
            coEvery { searchVocabulary(any(), any(), capture(learningOnly), any(), any()) } returns persistentListOf(kot)
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onToLearnToggled()
            advanceUntilIdle()

            assertTrue("the filter has to reach the query", learningOnly.captured)
            assertTrue(loaded(viewModel).toLearnOnly)
        }
}
