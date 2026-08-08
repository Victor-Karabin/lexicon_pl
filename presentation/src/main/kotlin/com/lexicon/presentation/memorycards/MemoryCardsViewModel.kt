package com.lexicon.presentation.memorycards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.memorycards.MemoryCardsStepOutcome
import com.lexicon.interactors.memorycards.MemoryCardsStepResponse
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionRequest
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionUseCase
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultRequest
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 400L
private const val INCORRECT_FLASH_DELAY_MS = 600L

@HiltViewModel
class MemoryCardsViewModel
    @Inject
    constructor(
        private val startSessionUseCase: StartMemoryCardsSessionUseCase,
        private val submitStepResultUseCase: SubmitMemoryCardsStepResultUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MemoryCardsUiState>(MemoryCardsUiState.Loading)
        val uiState: StateFlow<MemoryCardsUiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
        val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

        private lateinit var sessionId: String
        private var steps: List<MemoryCardsStepResponse> = emptyList()
        private var correctCount = 0
        private var incorrectCount = 0
        private var skippedCount = 0

        init {
            startSession()
        }

        private fun startSession() {
            viewModelScope.launch(dispatchers.io) {
                val response = startSessionUseCase(StartMemoryCardsSessionRequest())
                sessionId = response.sessionId
                steps = response.steps
                openStep(0)
            }
        }

        private fun openStep(index: Int) {
            val step = steps.getOrNull(index) ?: return
            val cards =
                step.pairs.flatMapIndexed { pairIndex, pair ->
                    listOf(
                        MemoryCard(
                            cardId = pairIndex * 2,
                            vocabularyItemId = pair.vocabularyItemId,
                            isImageCard = true,
                            imageUrl = pair.imageUrl,
                            text = pair.imageFallbackText,
                        ),
                        MemoryCard(
                            cardId = pairIndex * 2 + 1,
                            vocabularyItemId = pair.vocabularyItemId,
                            isImageCard = false,
                            imageUrl = null,
                            text = pair.text,
                        ),
                    )
                }.shuffled()
            _uiState.update {
                MemoryCardsUiState.Loaded(stepIndex = index, totalSteps = steps.size, cards = cards)
            }
        }

        fun onCardSelected(cardId: Int) {
            val state = _uiState.value as? MemoryCardsUiState.Loaded ?: return
            val card = state.cards.firstOrNull { it.cardId == cardId } ?: return
            if (!state.isInteractive) return
            if (state.matchedItemIds.contains(card.vocabularyItemId)) return
            if (state.flippedCardIds.contains(cardId)) return
            if (state.flippedCardIds.size >= 2) return

            val flipped = state.flippedCardIds + cardId
            updateLoaded { it.copy(flippedCardIds = flipped) }
            if (flipped.size == 2) validatePair(flipped[0], flipped[1])
        }

        private fun validatePair(
            firstCardId: Int,
            secondCardId: Int,
        ) {
            val state = _uiState.value as? MemoryCardsUiState.Loaded ?: return
            val first = state.cards.first { it.cardId == firstCardId }
            val second = state.cards.first { it.cardId == secondCardId }

            if (first.vocabularyItemId == second.vocabularyItemId) {
                val matched = state.matchedItemIds + first.vocabularyItemId
                updateLoaded { it.copy(matchedItemIds = matched, flippedCardIds = emptyList()) }
                if (matched.size == state.cards.size / 2) {
                    viewModelScope.launch(dispatchers.io) { completeStep() }
                }
            } else {
                updateLoaded {
                    it.copy(
                        incorrectAttempts = it.incorrectAttempts + 1,
                        incorrectFlashCardIds = setOf(firstCardId, secondCardId),
                    )
                }
                viewModelScope.launch {
                    delay(INCORRECT_FLASH_DELAY_MS)
                    updateLoaded { it.copy(flippedCardIds = emptyList(), incorrectFlashCardIds = emptySet()) }
                }
            }
        }

        private suspend fun completeStep() {
            val step = currentStepOrNull() ?: return
            val state = _uiState.value as? MemoryCardsUiState.Loaded ?: return
            val response =
                submitStepResultUseCase(
                    SubmitMemoryCardsStepResultRequest(
                        sessionId = sessionId,
                        stepIndex = step.stepIndex,
                        vocabularyItemIds = step.pairs.map { it.vocabularyItemId },
                        incorrectAttempts = state.incorrectAttempts,
                        skipped = false,
                    ),
                )
            when (response.outcome) {
                MemoryCardsStepOutcome.CORRECT -> correctCount++
                MemoryCardsStepOutcome.INCORRECT -> incorrectCount++
                MemoryCardsStepOutcome.SKIPPED -> Unit
            }
            delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
            advanceToNextStep()
        }

        fun onSkip() {
            val state = _uiState.value as? MemoryCardsUiState.Loaded ?: return
            if (!state.canSkip) return
            val step = currentStepOrNull() ?: return
            viewModelScope.launch(dispatchers.io) {
                submitStepResultUseCase(
                    SubmitMemoryCardsStepResultRequest(
                        sessionId = sessionId,
                        stepIndex = step.stepIndex,
                        vocabularyItemIds = step.pairs.map { it.vocabularyItemId },
                        incorrectAttempts = state.incorrectAttempts,
                        skipped = true,
                    ),
                )
                skippedCount++
                updateLoaded {
                    it.copy(
                        matchedItemIds = step.pairs.map { pair -> pair.vocabularyItemId }.toSet(),
                        answerState = AnswerState.Skipped(),
                    )
                }
            }
        }

        fun onNext() {
            val state = _uiState.value as? MemoryCardsUiState.Loaded ?: return
            if (!state.awaitingNext) return
            viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
        }

        private suspend fun advanceToNextStep() {
            val state = _uiState.value as? MemoryCardsUiState.Loaded ?: return
            val nextIndex = state.stepIndex + 1
            if (nextIndex >= steps.size) {
                updateLoaded { it.copy(isSessionComplete = true) }
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount))
                return
            }
            openStep(nextIndex)
        }

        private fun currentStepOrNull(): MemoryCardsStepResponse? {
            val state = _uiState.value as? MemoryCardsUiState.Loaded ?: return null
            return steps.getOrNull(state.stepIndex)
        }

        private inline fun updateLoaded(transform: (MemoryCardsUiState.Loaded) -> MemoryCardsUiState.Loaded) {
            _uiState.update { current -> if (current is MemoryCardsUiState.Loaded) transform(current) else current }
        }
    }
