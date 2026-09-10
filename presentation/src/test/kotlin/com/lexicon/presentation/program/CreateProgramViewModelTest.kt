package com.lexicon.presentation.program

import androidx.lifecycle.SavedStateHandle
import com.lexicon.interactors.program.CountStudySetUseCase
import com.lexicon.interactors.program.CreateProgramUseCase
import com.lexicon.interactors.program.DeleteProgramUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramDraft
import com.lexicon.interactors.program.ProgramEnrolment
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val STUDY_SET = 25

@OptIn(ExperimentalCoroutinesApi::class)
class CreateProgramViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private var creations = 0
    private var outcome: Result<Program> = Result.failure(IllegalStateException("not set"))

    private val createProgram =
        object : CreateProgramUseCase {
            override suspend fun invoke(draft: ProgramDraft): Result<Program> {
                creations++
                return outcome
            }
        }

    private val noActiveEnrolment =
        object : ObserveActiveEnrolmentUseCase {
            override fun invoke(): Flow<ProgramEnrolment?> = flowOf(null)
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        CreateProgramViewModel(
            savedStateHandle = SavedStateHandle(),
            createProgram = createProgram,
            updateProgram = mockk(),
            getProgram = mockk(),
            countStudySet = CountStudySetUseCase { STUDY_SET },
            enrol = mockk(),
            leave = mockk(),
            resetProgram = mockk(),
            deleteProgram = DeleteProgramUseCase { },
            observeActiveEnrolment = noActiveEnrolment,
        )

    @Test
    fun `tapping save twice creates the program once`() =
        runTest(testDispatcher) {
            outcome = Result.success(mockk(relaxed = true))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTrainingAdded("dictation")

            viewModel.onSave(name = "Mine", description = "")
            viewModel.onSave(name = "Mine", description = "")
            advanceUntilIdle()

            assertEquals(1, creations)
            assertTrue(viewModel.uiState.value.isSaved)
        }

    @Test
    fun `save cannot be tapped again while it is running`() =
        runTest(testDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTrainingAdded("dictation")

            viewModel.onSave(name = "Mine", description = "")

            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `a refused save can be tried again`() =
        runTest(testDispatcher) {
            outcome = Result.failure(IllegalStateException("refused"))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTrainingAdded("dictation")

            viewModel.onSave(name = "Mine", description = "")
            advanceUntilIdle()
            viewModel.onSave(name = "Mine", description = "")
            advanceUntilIdle()

            assertEquals(2, creations)
            assertFalse(viewModel.uiState.value.isSaving)
        }
}
