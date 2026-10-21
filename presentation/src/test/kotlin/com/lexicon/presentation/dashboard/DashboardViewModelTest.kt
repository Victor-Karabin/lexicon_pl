package com.lexicon.presentation.dashboard

import app.cash.turbine.test
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.countries.CountryCourse
import com.lexicon.interactors.countries.LoadCountryCourseUseCase
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.declension.DeclensionCourse
import com.lexicon.interactors.declension.LoadDeclensionCoursesUseCase
import com.lexicon.interactors.numbers.LoadNumberCourseUseCase
import com.lexicon.interactors.numbers.NumberCourse
import com.lexicon.interactors.presets.CountWordsToReviewUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.vocabularycourse.CourseLaunch
import com.lexicon.interactors.vocabularycourse.GetCourseProgressUseCase
import com.lexicon.interactors.vocabularycourse.GetDailyStudyTimeUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.ObserveVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.StudyTimeHistory
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabularycourse.CourseProgress
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val getProgress: GetCourseProgressUseCase = mockk()
    private val queue: NextCourseTrainingUseCase = mockk()
    private val loadConjugationCourses: LoadConjugationCoursesUseCase = mockk()
    private val loadDeclensionCourses: LoadDeclensionCoursesUseCase = mockk()
    private val loadNumberCourse: LoadNumberCourseUseCase = mockk()
    private val loadCountryCourse: LoadCountryCourseUseCase = mockk()
    private val countWordsToReview: CountWordsToReviewUseCase = mockk()
    private val getDailyStudyTime: GetDailyStudyTimeUseCase = mockk()
    private val observeCourse: ObserveVocabularyCourseUseCase = mockk()
    private val observeCourses: ObserveCoursesUseCase = mockk { coEvery { this@mockk() } returns emptyFlow() }
    private val observeWordStatuses: ObserveWordStatusesUseCase = mockk()
    private val wordStatuses = MutableSharedFlow<Map<VocabularyId, WordStatus>>()

    private val words = persistentListOf(VocabularyId(1), VocabularyId(2))
    private val progress = CourseProgress(dueWords = 3)
    private val studyTime = StudyTimeHistory(persistentListOf())

    private val withCardsNext = VocabularyCourse(cardsSeen = false, newWords = words)
    private val withoutCardsNext = VocabularyCourse(cardsSeen = true, newWords = words)

    private var conjugation: ImmutableList<ConjugationCourse> = persistentListOf(mockk())
    private var declension: ImmutableList<DeclensionCourse> = persistentListOf(mockk())
    private var numbers: NumberCourse? = mockk()
    private var countries: CountryCourse? = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { getProgress() } returns progress
        coEvery { loadConjugationCourses() } answers { conjugation }
        coEvery { loadDeclensionCourses() } answers { declension }
        coEvery { loadNumberCourse() } answers { numbers }
        coEvery { loadCountryCourse() } answers { countries }
        coEvery { countWordsToReview() } returns 5
        coEvery { getDailyStudyTime() } returns studyTime
        coEvery { observeWordStatuses() } returns wordStatuses
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel(course: Flow<VocabularyCourse>): DashboardViewModel {
        coEvery { observeCourse() } returns course
        return DashboardViewModel(
            getProgress,
            queue,
            loadConjugationCourses,
            loadDeclensionCourses,
            loadNumberCourse,
            loadCountryCourse,
            countWordsToReview,
            getDailyStudyTime,
            observeCourse,
            observeCourses,
            observeWordStatuses,
        )
    }

    private fun TestScope.viewModel(course: VocabularyCourse = withoutCardsNext): DashboardViewModel =
        newViewModel(flowOf(course)).also { advanceUntilIdle() }

    @Test
    fun `the first state with a course already carries the progress and all four course groups`() =
        runTest(dispatcher) {
            val viewModel = newViewModel(flowOf(withoutCardsNext))

            viewModel.uiState.test {
                assertTrue(awaitItem().isLoading)
                advanceUntilIdle()

                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertEquals(withoutCardsNext, loaded.course)
                assertEquals(progress, loaded.progress)
                assertEquals(conjugation, loaded.conjugationCourses)
                assertEquals(declension, loaded.declensionCourses)
                assertEquals(numbers, loaded.numberCourse)
                assertEquals(countries, loaded.countryCourse)
                assertFalse(loaded.nothingToPractise)
                expectNoEvents()
            }
        }

    @Test
    fun `refreshing the courses reloads the four groups and leaves the rest alone`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            conjugation = persistentListOf(mockk(), mockk())
            declension = persistentListOf()
            numbers = null
            countries = mockk()

            viewModel.refreshCourses()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(conjugation, state.conjugationCourses)
            assertEquals(declension, state.declensionCourses)
            assertNull(state.numberCourse)
            assertEquals(countries, state.countryCourse)
            assertEquals(withoutCardsNext, state.course)
            assertEquals(progress, state.progress)
            assertFalse(state.isLoading)
        }

    @Test
    fun `the second word-status emission refreshes the courses and the first is dropped`() =
        runTest(dispatcher) {
            viewModel()
            coVerify(exactly = 1) { loadNumberCourse() }

            wordStatuses.emit(emptyMap())
            advanceUntilIdle()
            coVerify(exactly = 1) { loadNumberCourse() }

            conjugation = persistentListOf()
            wordStatuses.emit(mapOf(VocabularyId(1) to WordStatus.TO_LEARN))
            advanceUntilIdle()

            coVerify(exactly = 2) { loadNumberCourse() }
            coVerify(exactly = 2) { loadConjugationCourses() }
            coVerify(exactly = 2) { loadDeclensionCourses() }
            coVerify(exactly = 2) { loadCountryCourse() }
        }

    @Test
    fun `continuing when the new-word cards come first opens them without asking the queue`() =
        runTest(dispatcher) {
            val viewModel = viewModel(withCardsNext)

            viewModel.onContinue()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.openCards)
            assertNull(viewModel.uiState.value.launch)
            assertFalse(viewModel.uiState.value.nothingToPractise)
            coVerify(exactly = 0) { queue.next() }
        }

    @Test
    fun `continuing launches whatever the queue says is next`() =
        runTest(dispatcher) {
            coEvery { queue.next() } returns CourseLaunch(TrainingType.DICTATION, words)
            val viewModel = viewModel()

            viewModel.onContinue()
            advanceUntilIdle()

            val launch = viewModel.uiState.value.launch
            assertEquals(TrainingType.DICTATION, launch?.training)
            assertEquals(words, launch?.wordIds)
            assertFalse(viewModel.uiState.value.openCards)
            assertFalse(viewModel.uiState.value.nothingToPractise)
        }

    @Test
    fun `continuing with nothing in the queue says there is nothing to practise`() =
        runTest(dispatcher) {
            coEvery { queue.next() } returns null
            val viewModel = viewModel()

            viewModel.onContinue()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.nothingToPractise)
            assertNull(viewModel.uiState.value.launch)
            assertFalse(viewModel.uiState.value.openCards)
        }

    @Test
    fun `continuing before the course has loaded does nothing`() =
        runTest(dispatcher) {
            val viewModel = newViewModel(emptyFlow())
            advanceUntilIdle()

            viewModel.onContinue()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.openCards)
            assertFalse(viewModel.uiState.value.nothingToPractise)
            coVerify(exactly = 0) { queue.next() }
        }

    @Test
    fun `handling a launch clears the launch`() =
        runTest(dispatcher) {
            coEvery { queue.next() } returns CourseLaunch(TrainingType.DICTATION, words)
            val viewModel = viewModel()
            viewModel.onContinue()
            advanceUntilIdle()

            viewModel.onLaunchHandled()

            assertNull(viewModel.uiState.value.launch)
        }

    @Test
    fun `handling the cards request clears it`() =
        runTest(dispatcher) {
            val viewModel = viewModel(withCardsNext)
            viewModel.onContinue()
            advanceUntilIdle()

            viewModel.onLaunchHandled()

            assertFalse(viewModel.uiState.value.openCards)
        }

    @Test
    fun `coming back to the screen clears the nothing-to-practise notice and refreshes the figures`() =
        runTest(dispatcher) {
            coEvery { queue.next() } returns null
            val viewModel = viewModel()
            viewModel.onContinue()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.nothingToPractise)

            viewModel.onResumed()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.nothingToPractise)
            assertEquals(studyTime, state.studyTime)
            assertEquals(5, state.wordsToReview)
            assertEquals(progress, state.progress)
        }
}
