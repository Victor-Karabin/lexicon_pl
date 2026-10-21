package com.lexicon.presentation.presets

import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.ObserveVocabularyPresetsUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class VocabularyViewModelStatusTest {
    private val dispatcher = StandardTestDispatcher()

    private val searchVocabulary: SearchVocabularyUseCase = mockk {
        coEvery { this@mockk(any(), any(), any()) } returns persistentListOf()
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        observePresets: ObserveVocabularyPresetsUseCase,
        observeStatuses: ObserveWordStatusesUseCase,
    ) = VocabularyViewModel(
        observePresets = observePresets,
        searchVocabulary = searchVocabulary,
        setWordStatus = mockk(relaxed = true),
        deleteWord = mockk(relaxed = true),
        restoreWord = mockk(relaxed = true),
        deletePreset = mockk(relaxed = true),
        restorePreset = mockk(relaxed = true),
        observeWordStatuses = observeStatuses,
        getWordPresetMemberships = mockk(relaxed = true),
        setWordPresetMembership = mockk(relaxed = true),
        dispatchers = object : DispatcherProvider {
            override val io: CoroutineDispatcher get() = dispatcher
            override val default: CoroutineDispatcher get() = dispatcher
            override val main: CoroutineDispatcher get() = dispatcher
        },
        speechSynthesizer = mockk(relaxed = true),
    )

    private val statuses = mapOf(VocabularyId(1L) to WordStatus.TO_LEARN, VocabularyId(2L) to WordStatus.KNOWN)

    @Test
    fun `statuses that arrive before the presets are still shown`() =
        runTest(dispatcher) {
            val slowPresets: ObserveVocabularyPresetsUseCase = mockk {
                every { this@mockk() } returns flow {
                    delay(PRESETS_DELAY_MS)
                    emit(persistentListOf())
                }
            }
            val fastStatuses: ObserveWordStatusesUseCase = mockk { every { this@mockk() } returns flowOf(statuses) }

            val viewModel = viewModel(slowPresets, fastStatuses)
            advanceUntilIdle()

            assertEquals(statuses, (viewModel.uiState.value as VocabularyUiState.Loaded).wordStatuses)
        }

    @Test
    fun `statuses that arrive after the presets are shown`() =
        runTest(dispatcher) {
            val fastPresets: ObserveVocabularyPresetsUseCase = mockk { every { this@mockk() } returns flowOf(persistentListOf()) }
            val slowStatuses: ObserveWordStatusesUseCase = mockk {
                every { this@mockk() } returns flow {
                    delay(PRESETS_DELAY_MS)
                    emit(statuses)
                }
            }

            val viewModel = viewModel(fastPresets, slowStatuses)
            advanceUntilIdle()

            assertEquals(statuses, (viewModel.uiState.value as VocabularyUiState.Loaded).wordStatuses)
        }

    private companion object {
        const val PRESETS_DELAY_MS = 100L
    }
}
