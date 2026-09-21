package com.lexicon.presentation.review

import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.CountWordsToReviewUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetWordsToReviewUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.interactors.vocabularycourse.GetWordCardsUseCase
import com.lexicon.interactors.vocabularycourse.WordCard
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

private const val BEFORE_IT_SETTLES_MS = 100L
private const val AFTER_IT_SETTLES_MS = 500L

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewWordsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val kot = Word(VocabularyId(1L), "kot", "cat", "kɔt")
    private val pies = Word(VocabularyId(2L), "pies", "dog", "pjɛs")

    private val getWordsToReview: GetWordsToReviewUseCase = mockk {
        coEvery { this@mockk(any()) } returns persistentListOf(kot, pies) andThen persistentListOf()
    }
    private val countWordsToReview: CountWordsToReviewUseCase = mockk {
        coEvery { this@mockk() } returns 2
    }
    private val setWordStatus: SetWordStatusUseCase = mockk(relaxed = true)
    private val deleteWord: DeleteWordUseCase = mockk(relaxed = true)
    private val getWordCards: GetWordCardsUseCase = mockk {
        coEvery { this@mockk(any()) } answers {
            firstArg<List<VocabularyId>>().map { id ->
                WordCard(id = id, text = "", translation = "", transcription = "", imageUrl = "https://img/${id.value}.jpg")
            }.toImmutableList()
        }
    }

    private fun viewModel() =
        ReviewWordsViewModel(
            getWordsToReview = getWordsToReview,
            countWordsToReview = countWordsToReview,
            setWordStatus = setWordStatus,
            deleteWord = deleteWord,
            getWordCards = getWordCards,
            speechSynthesizer = mockk(relaxed = true),
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

    @Test
    fun `only words with nothing set are offered, newest count first`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(kot, viewModel.uiState.value.current)
            assertEquals(2, viewModel.uiState.value.waiting)
        }

    @Test
    fun `choosing a status writes it and moves on by itself`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onStatusChosen(WordStatus.KNOWN)
            advanceTimeBy(AFTER_IT_SETTLES_MS)

            coVerify { setWordStatus(kot.id, WordStatus.KNOWN) }
            assertEquals(pies, viewModel.uiState.value.current)
            assertEquals(1, viewModel.uiState.value.reviewed)
            assertEquals(1, viewModel.uiState.value.waiting)
        }

    @Test
    fun `changing your mind before it settles writes the second choice only`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onStatusChosen(WordStatus.KNOWN)
            advanceTimeBy(BEFORE_IT_SETTLES_MS)
            viewModel.onStatusChosen(WordStatus.TO_LEARN)
            advanceTimeBy(AFTER_IT_SETTLES_MS)

            coVerify(exactly = 0) { setWordStatus(kot.id, WordStatus.KNOWN) }
            coVerify(exactly = 1) { setWordStatus(kot.id, WordStatus.TO_LEARN) }
            assertEquals(pies, viewModel.uiState.value.current)
        }

    @Test
    fun `the choice shows on the card while it settles`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onStatusChosen(WordStatus.FAVOURITE)
            advanceTimeBy(BEFORE_IT_SETTLES_MS)

            assertEquals(WordStatus.FAVOURITE, viewModel.uiState.value.chosen)
            assertEquals(kot, viewModel.uiState.value.current)
        }

    @Test
    fun `deleting drops the word and moves on`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDeleted()
            advanceTimeBy(AFTER_IT_SETTLES_MS)

            coVerify { deleteWord(kot.id) }
            assertEquals(pies, viewModel.uiState.value.current)
        }

    @Test
    fun `the queue is refilled when the batch runs out, and ends when nothing is left`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onStatusChosen(WordStatus.KNOWN)
            advanceTimeBy(AFTER_IT_SETTLES_MS)
            viewModel.onStatusChosen(WordStatus.KNOWN)
            advanceTimeBy(AFTER_IT_SETTLES_MS)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.current)
            assertTrue(viewModel.uiState.value.isFinished)
            assertEquals(2, viewModel.uiState.value.reviewed)
        }

    @Test
    fun `the card shows the word's picture, and the next word's is fetched ahead`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals("https://img/1.jpg", viewModel.uiState.value.currentPicture)
            coVerify(exactly = 1) { getWordCards(listOf(kot.id, pies.id)) }

            viewModel.onStatusChosen(WordStatus.KNOWN)
            advanceTimeBy(AFTER_IT_SETTLES_MS)
            advanceUntilIdle()

            assertEquals("https://img/2.jpg", viewModel.uiState.value.currentPicture)
            coVerify(exactly = 1) { getWordCards(any()) }
        }
}
