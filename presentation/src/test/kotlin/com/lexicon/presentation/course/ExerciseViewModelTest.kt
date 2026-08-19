package com.lexicon.presentation.course

import androidx.lifecycle.SavedStateHandle
import com.lexicon.boundary.LessonAudioLibrary
import com.lexicon.boundary.LessonAudioPlayer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.CheckExerciseAnswerUseCase
import com.lexicon.interactors.course.GapFillItem
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonAudio
import com.lexicon.interactors.course.LessonExercise
import com.lexicon.interactors.course.MinimalPairItem
import com.lexicon.model.course.CourseId
import com.lexicon.model.course.LessonId
import com.lexicon.presentation.common.AnswerState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExerciseViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val getLesson: GetLessonUseCase = mockk()

    private val checkAnswer =
        object : CheckExerciseAnswerUseCase {
            override fun invoke(
                expected: String,
                submitted: String,
            ): Boolean = expected.trim().lowercase() == submitted.trim().lowercase()
        }
    private val audioLibrary: LessonAudioLibrary = mockk()
    private val playingFile = MutableStateFlow<String?>(null)
    private val audioPlayer: LessonAudioPlayer = mockk(relaxed = true)

    private val minimalPair =
        LessonExercise.MinimalPair(
            id = "lesson-1-101a1",
            instruction = "Co mówi lektor?",
            audioFile = "101a1.mp3",
            items =
                persistentListOf(
                    MinimalPairItem(label = "a", options = persistentListOf("kot", "kod"), answer = "kot"),
                    MinimalPairItem(label = "b", options = persistentListOf("pies", "piec"), answer = "piec"),
                ),
        )

    private val gapFill =
        LessonExercise.GapFill(
            id = "lesson-1-101a2",
            instruction = "posłuchać i uzupełnić",
            audioFile = "101a2.mp3",
            items =
                persistentListOf(
                    GapFillItem(prompt = "Dzień dobry, ___ się Mami", answers = persistentListOf("Nazywam")),
                ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { audioPlayer.playingFile } returns playingFile
        every { audioPlayer.pause() } answers { playingFile.value = null }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun lesson(vararg exercises: LessonExercise) =
        Lesson(
            id = LessonId("lesson-1"),
            courseId = CourseId("krok-a1"),
            number = 1,
            title = "Lekcja 1",
            vocabularyIds = persistentListOf(),
            audio = exercises.mapNotNull { it.audioFile }.map { LessonAudio(it, null, 0, null, "drive-$it") }.toImmutableList(),
            exercises = exercises.toList().toImmutableList(),
            isCompleted = false,
        )

    private fun TestScope.viewModel(exerciseId: String): ExerciseViewModel {
        val viewModel =
            ExerciseViewModel(
                SavedStateHandle(mapOf(LESSON_ID_ARG to "lesson-1", EXERCISE_ID_ARG to exerciseId)),
                getLesson,
                checkAnswer,
                audioLibrary,
                audioPlayer,
                dispatchers,
            )
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `loads the exercise matching the id in the route`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair, gapFill)

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ExerciseUiState.Loaded
            assertEquals(minimalPair.id, state.exercise.id)
        }

    @Test
    fun `an exercise id absent from the lesson is Not Found`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)

            val viewModel = viewModel(exerciseId = "no-such-exercise")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(ExerciseUiState.NotFound, viewModel.uiState.value)
        }

    @Test
    fun `checking a minimal pair scores each selection and marks Correct when all match`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onOptionSelected(0, "kot")
            viewModel.onOptionSelected(1, "piec")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ExerciseUiState.Loaded
            assertEquals(AnswerState.Correct, state.answerState)
            assertEquals(2, state.correctCount)
        }

    @Test
    fun `one wrong selection out of two is Incorrect with partial credit`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onOptionSelected(0, "kot")
            viewModel.onOptionSelected(1, "pies")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ExerciseUiState.Loaded
            assertTrue(state.answerState is AnswerState.Incorrect)
            assertEquals(1, state.correctCount)
        }

    @Test
    fun `an unanswered question counts as wrong, not skipped`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onOptionSelected(0, "kot")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ExerciseUiState.Loaded
            assertEquals(1, state.correctCount)
        }

    @Test
    fun `gap fill checking ignores case and surrounding whitespace, same as the trainings`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(gapFill)

            val viewModel = viewModel(exerciseId = gapFill.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onGapChanged(0, 0, "  nazywam  ")
            viewModel.onCheck()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ExerciseUiState.Loaded
            assertEquals(AnswerState.Correct, state.answerState)
        }

    @Test
    fun `retry clears responses and score back to unanswered`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onOptionSelected(0, "kot")
            viewModel.onOptionSelected(1, "piec")
            viewModel.onCheck()
            viewModel.onRetry()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ExerciseUiState.Loaded
            assertEquals(AnswerState.Unanswered, state.answerState)
            assertEquals(0, state.correctCount)
            assertTrue(state.responses.all { row -> row.all { it.isEmpty() } })
        }

    @Test
    fun `playing audio fetches the track using the lesson's remote id, then plays it`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)
            coEvery { audioLibrary.pathOrNull("101a1.mp3", "drive-101a1.mp3") } returns "/cache/101a1.mp3"
            coEvery { audioPlayer.play("101a1.mp3", "/cache/101a1.mp3") } returns Unit

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onPlayAudio()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { audioPlayer.play("101a1.mp3", "/cache/101a1.mp3") }
        }

    @Test
    fun `a track that cannot be fetched is reported as missing rather than crashing`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)
            coEvery { audioLibrary.pathOrNull("101a1.mp3", "drive-101a1.mp3") } returns null

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onPlayAudio()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as ExerciseUiState.Loaded
            assertTrue(state.isAudioMissing)
        }

    @Test
    fun `tapping play while the track is already playing pauses it instead of restarting`() =
        runTest {
            coEvery { getLesson(LessonId("lesson-1")) } returns lesson(minimalPair)
            playingFile.value = "101a1.mp3"

            val viewModel = viewModel(exerciseId = minimalPair.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onPlayAudio()

            assertNull(playingFile.value)
        }
}
