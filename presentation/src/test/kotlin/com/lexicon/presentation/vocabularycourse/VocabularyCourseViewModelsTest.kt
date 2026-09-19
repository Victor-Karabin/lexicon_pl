package com.lexicon.presentation.vocabularycourse

import com.lexicon.interactors.vocabularycourse.CourseLaunch
import com.lexicon.interactors.vocabularycourse.CourseSettings
import com.lexicon.interactors.vocabularycourse.GetVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.ResetCourseQueueUseCase
import com.lexicon.interactors.vocabularycourse.UpdateCourseSettingsUseCase
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyCourseViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    private val words = persistentListOf(VocabularyId(1), VocabularyId(2))
    private val settings = CourseSettings(newWordsADay = 10, reviewsADay = 15, queue = listOf("dictation", "word_match"))

    private val getCourse: GetVocabularyCourseUseCase = mockk {
        coEvery { this@mockk() } returns VocabularyCourse(settings = settings, cardsSeen = true, newWords = words)
    }
    private val updateSettings: UpdateCourseSettingsUseCase = mockk(relaxed = true)
    private val queue: NextCourseTrainingUseCase = mockk()
    private val resetQueue: ResetCourseQueueUseCase = mockk(relaxed = true)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the settings screen opens on what the course holds`() =
        runTest(dispatcher) {
            val viewModel = CourseSettingsViewModel(getCourse, updateSettings)
            advanceUntilIdle()

            assertEquals(10, viewModel.uiState.value.newWordsADay)
            assertEquals(15, viewModel.uiState.value.reviewsADay)
            assertEquals(listOf("dictation", "word_match"), viewModel.uiState.value.queue)
        }

    @Test
    fun `closing the settings stores them before the screen goes`() =
        runTest(dispatcher) {
            val viewModel = CourseSettingsViewModel(getCourse, updateSettings)
            advanceUntilIdle()
            val close = mockk<() -> Unit>(relaxed = true)
            val saved = slot<CourseSettings>()
            coEvery { updateSettings(capture(saved)) } returns Unit

            viewModel.onNewWordsChanged(20)
            viewModel.onTurnRemoved(0)
            viewModel.onDone(close)
            advanceUntilIdle()

            coVerifyOrder {
                updateSettings(any())
                close()
            }
            assertEquals(20, saved.captured.newWordsADay)
            assertEquals(listOf("word_match"), saved.captured.queue)
        }

    @Test
    fun `the last training in the queue cannot be removed`() =
        runTest(dispatcher) {
            val viewModel = CourseSettingsViewModel(getCourse, updateSettings)
            advanceUntilIdle()

            viewModel.onTurnRemoved(0)
            viewModel.onTurnRemoved(0)

            assertEquals(listOf("word_match"), viewModel.uiState.value.queue)
            assertTrue(viewModel.uiState.value.keptLastTraining)
        }

    @Test
    fun `reset starts the queue again at its first training`() =
        runTest(dispatcher) {
            coEvery { queue.next() } returns CourseLaunch(TrainingType.DICTATION, words)
            val viewModel = CourseRunViewModel(queue, resetQueue, getCourse)

            viewModel.onReset()
            advanceUntilIdle()

            coVerify { resetQueue() }
            assertEquals(CourseRunStep.Next(TrainingType.DICTATION, words), viewModel.step.value)
        }

    @Test
    fun `reset goes to the new-word cards when a round opens with them`() =
        runTest(dispatcher) {
            coEvery { getCourse() } returns VocabularyCourse(settings = settings, cardsSeen = false, newWords = words)
            val viewModel = CourseRunViewModel(queue, resetQueue, getCourse)

            viewModel.onReset()
            advanceUntilIdle()

            assertEquals(CourseRunStep.Cards, viewModel.step.value)
        }

    @Test
    fun `finishing a training with nothing left to run goes back home`() =
        runTest(dispatcher) {
            coEvery { queue.advance() } returns null
            val viewModel = CourseRunViewModel(queue, resetQueue, getCourse)

            viewModel.onTrainingFinished()
            advanceUntilIdle()

            assertEquals(CourseRunStep.NothingToPractise, viewModel.step.value)
        }
}
