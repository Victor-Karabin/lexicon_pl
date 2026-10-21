package com.lexicon.presentation.wordcard

import androidx.lifecycle.SavedStateHandle
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.vocabularycourse.GetWordCardsUseCase
import com.lexicon.interactors.vocabularycourse.WordCard
import com.lexicon.interactors.wordcard.RecordWordCardSeenUseCase
import com.lexicon.interactors.wordcard.StartWordCardSessionUseCase
import com.lexicon.interactors.wordcard.WordCardSessionResponse
import com.lexicon.interactors.wordcard.WordCardStep
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WordCardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val startSession: StartWordCardSessionUseCase = mockk()
    private val recordSeen: RecordWordCardSeenUseCase = mockk(relaxed = true)
    private val getWordCards: GetWordCardsUseCase = mockk()
    private val speechSynthesizer: SpeechSynthesizer = mockk(relaxed = true)

    private fun step(
        index: Int,
        text: String = "slowo$index",
    ) = WordCardStep(
        stepIndex = index,
        vocabularyItemId = 10L + index,
        text = text,
        translation = "word $index",
        transcription = "",
        imageUrl = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { startSession(any()) } returns WordCardSessionResponse("session-1", listOf(step(0), step(1), step(2)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        WordCardViewModel(
            savedStateHandle = SavedStateHandle(),
            startSession = startSession,
            recordSeen = recordSeen,
            getWordCards = getWordCards,
            speechSynthesizer = speechSynthesizer,
            dispatchers = dispatchers,
        )

    @Test
    fun `the session is started once and its first card is recorded once`() {
        viewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { startSession(any()) }
        coVerify(exactly = 1) { recordSeen(match { it.vocabularyItemId == 10L }) }
    }

    @Test
    fun `after editing a word only that card is refreshed, the rest and the place are kept`() {
        val viewModel = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onNext()
        testDispatcher.scheduler.advanceUntilIdle()
        val before = viewModel.uiState.value
        coEvery { getWordCards(listOf(VocabularyId(11))) } returns persistentListOf(
            WordCard(id = VocabularyId(11), text = "poprawione", translation = "fixed", transcription = "", imageUrl = null, example = ""),
        )

        viewModel.onWordEdited(11)
        testDispatcher.scheduler.advanceUntilIdle()

        val after = viewModel.uiState.value
        assertEquals(1, after.index)
        assertEquals(listOf("slowo0", "poprawione", "slowo2"), after.cards.map { it.text })
        assertEquals(before.cards[0], after.cards[0])
        assertEquals(before.cards[2], after.cards[2])
        assertEquals(1, after.cards[1].stepIndex)
        assertEquals(11L, after.cards[1].vocabularyItemId)
        coVerify(exactly = 1) { startSession(any()) }
    }

    @Test
    fun `refreshing an edited card does not record the card as seen again`() {
        val viewModel = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { getWordCards(listOf(VocabularyId(10))) } returns persistentListOf(
            WordCard(id = VocabularyId(10), text = "poprawione", translation = "fixed", transcription = "", imageUrl = null, example = ""),
        )

        viewModel.onWordEdited(10)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { recordSeen(any()) }
    }

    @Test
    fun `editing a word that is not in the session changes nothing`() {
        val viewModel = viewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val before = viewModel.uiState.value
        coEvery { getWordCards(any()) } returns persistentListOf()

        viewModel.onWordEdited(999)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, viewModel.uiState.value)
    }
}
