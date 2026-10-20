package com.lexicon.presentation.conjugation

import androidx.lifecycle.SavedStateHandle
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.conjugation.ConjugationCourseProgress
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.ConjugationVariantProgress
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.conjugation.NextConjugationTableUseCase
import com.lexicon.presentation.common.LastSessionResultsHolder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConjugationViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val nextQuestion: NextConjugationTableUseCase = mockk {
        coEvery { this@mockk(any()) } returns null
    }
    private val loadCourses: LoadConjugationCoursesUseCase = mockk()

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        ConjugationViewModel(
            SavedStateHandle(mapOf(CONJUGATION_COURSE_ARG to "course-1")),
            nextQuestion,
            mockk(),
            mockk(),
            mockk(),
            loadCourses,
            mockk(relaxed = true),
            LastSessionResultsHolder(),
            mockk(relaxed = true),
            dispatchers,
        )

    private fun course(streak: Int) =
        ConjugationCourse(
            id = "course-1",
            infinitives = persistentListOf("czytać"),
            progress = ConjugationCourseProgress(
                GrammaticalPerson.entries
                    .map { ConjugationVariantProgress(ConjugationVariant("czytać", it), streak = streak) }
                    .toImmutableList(),
            ),
        )

    @Test
    fun `a fully mastered course opens on the completed message`() =
        runTest(testDispatcher) {
            coEvery { loadCourses() } returns persistentListOf(course(ConjugationVariantProgress.MASTERY_STREAK))

            val viewModel = viewModel()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.table)
            assertTrue(viewModel.uiState.value.isComplete)
        }

    @Test
    fun `a course without verbs opens on the choose-verbs message`() =
        runTest(testDispatcher) {
            coEvery { loadCourses() } returns persistentListOf()

            val viewModel = viewModel()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.table)
            assertFalse(viewModel.uiState.value.isComplete)
        }
}
