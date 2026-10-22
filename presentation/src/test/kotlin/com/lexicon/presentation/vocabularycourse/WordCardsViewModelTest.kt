package com.lexicon.presentation.vocabularycourse

import com.lexicon.interactors.vocabularycourse.CourseLaunch
import com.lexicon.interactors.vocabularycourse.GetVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.GetWordCardsUseCase
import com.lexicon.interactors.vocabularycourse.MarkCourseCardsSeenUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.interactors.vocabularycourse.WordCard
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class WordCardsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val ids = persistentListOf(VocabularyId(1), VocabularyId(2), VocabularyId(3))
    private var cardsSeen = false
    private var cards: ImmutableList<WordCard> = ids.map(::card).toImmutableList()

    private val getCourse: GetVocabularyCourseUseCase = mockk {
        coEvery { this@mockk() } answers { VocabularyCourse(cardsSeen = cardsSeen, newWords = ids) }
    }
    private val getCards: GetWordCardsUseCase = mockk { coEvery { this@mockk(any()) } answers { cards } }
    private val markSeen: MarkCourseCardsSeenUseCase = mockk { coEvery { this@mockk() } answers { cardsSeen = true } }
    private val queue: NextCourseTrainingUseCase = mockk()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun card(id: VocabularyId) = WordCard(id, "słowo ${id.value}", "word ${id.value}", "x", imageUrl = null)

    private fun TestScope.viewModel() =
        WordCardsViewModel(getCourse, getCards, markSeen, queue, mockk(relaxed = true)).also { advanceUntilIdle() }

    @Test
    fun `the cards of the course's new words are shown from the first`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(cards, state.cards)
            assertEquals(0, state.index)
            assertEquals(cards.first(), state.current)
            assertFalse(state.isFinished)
        }

    @Test
    fun `loading again keeps the position but never beyond the last card`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onNext()
            viewModel.onNext()
            assertEquals(2, viewModel.uiState.value.index)

            viewModel.load()
            advanceUntilIdle()
            assertEquals(2, viewModel.uiState.value.index)

            cards = persistentListOf(card(VocabularyId(1)))
            viewModel.load()
            advanceUntilIdle()
            assertEquals(0, viewModel.uiState.value.index)
        }

    @Test
    fun `loading with no cards stays on the first position`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onNext()

            cards = persistentListOf()
            viewModel.load()
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.index)
            assertTrue(viewModel.uiState.value.cards.isEmpty())
        }

    @Test
    fun `previous stops at the first card`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onNext()

            viewModel.onPrevious()
            viewModel.onPrevious()
            viewModel.onPrevious()

            assertEquals(0, viewModel.uiState.value.index)
        }

    @Test
    fun `next before the last card only moves on`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onNext()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.index)
            assertFalse(viewModel.uiState.value.isFinished)
            assertNull(viewModel.uiState.value.launch)
            coVerify(exactly = 0) { markSeen() }
            coVerify(exactly = 0) { queue.next() }
        }

    @Test
    fun `next on the last card marks the cards seen, then finishes with the training that follows`() =
        runTest(dispatcher) {
            coEvery { queue.next() } returns CourseLaunch(TrainingType.IMAGE_TEST, ids)
            val viewModel = viewModel()
            viewModel.onNext()
            viewModel.onNext()
            assertTrue(viewModel.uiState.value.isLast)

            viewModel.onNext()
            advanceUntilIdle()

            coVerifyOrder {
                markSeen()
                queue.next()
            }
            val state = viewModel.uiState.value
            assertTrue(state.isFinished)
            assertEquals(TrainingType.IMAGE_TEST, state.launch?.training)
            assertEquals(ids, state.launch?.wordIds)
            assertEquals(2, state.index)
        }

    @Test
    fun `next on the last card with nothing to follow finishes without a launch`() =
        runTest(dispatcher) {
            coEvery { queue.next() } returns null
            val viewModel = viewModel()
            viewModel.onNext()
            viewModel.onNext()

            viewModel.onNext()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isFinished)
            assertNull(viewModel.uiState.value.launch)
            coVerify(exactly = 1) { markSeen() }
        }
}
