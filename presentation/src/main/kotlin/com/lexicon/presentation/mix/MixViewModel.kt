package com.lexicon.presentation.mix

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.recognition.SpeechRecognitionFailed
import com.lexicon.android.recognition.SpeechRecognizerService
import com.lexicon.android.speech.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerRequest
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import com.lexicon.interactors.mix.MixStep
import com.lexicon.interactors.mix.StartMixSessionRequest
import com.lexicon.interactors.mix.StartMixSessionUseCase
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerRequest
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.model.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.shuffleIntoTiles
import com.lexicon.presentation.common.trainingVocabularyIds
import com.lexicon.presentation.pronunciation.RecordingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 500L
private const val SKIPPED_ANSWER_ADVANCE_DELAY_MS = 700L

class MixViewModel(
    savedStateHandle: SavedStateHandle,
    private val startMixSession: StartMixSessionUseCase,
    private val submitDictation: SubmitDictationAnswerUseCase,
    private val submitDictationPuzzle: SubmitDictationPuzzleAnswerUseCase,
    private val submitPuzzle: SubmitPuzzleAnswerUseCase,
    private val submitImageTest: SubmitImageTestAnswerUseCase,
    private val submitTrueOrFalse: SubmitTrueOrFalseAnswerUseCase,
    private val submitPronunciation: SubmitPronunciationResultUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val speechRecognizerService: SpeechRecognizerService,
    private val dispatchers: DispatcherProvider,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
) : ViewModel() {
    private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

    private val _uiState = MutableStateFlow<MixUiState>(MixUiState.Loading)
    val uiState: StateFlow<MixUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private lateinit var sessionId: String
    private var steps: List<MixStep> = emptyList()
    private var correctCount = 0
    private var incorrectCount = 0
    private var skippedCount = 0
    private var tipsUsedCount = 0
    private val wordResults = mutableListOf<WordResultEntry>()

    init {
        startSession()
    }

    private fun startSession() {
        viewModelScope.launch(dispatchers.io) {
            val response = startMixSession(StartMixSessionRequest(vocabularyIds = vocabularyIds))
            sessionId = response.sessionId
            steps = response.steps
            openStep(0)
        }
    }

    private suspend fun openStep(index: Int) {
        val step = steps.getOrNull(index) ?: return
        _uiState.update {
            MixUiState.Loaded(
                stepIndex = index,
                totalSteps = steps.size,
                step = step,
                stepTiles = step.tilesOrEmpty(),
            )
        }
        if (step is MixStep.Dictation) speechSynthesizer.speak(step.step.expectedText)
        if (step is MixStep.DictationPuzzle) speechSynthesizer.speak(step.step.expectedText)
        if (step is MixStep.Pronunciation) speechSynthesizer.speak(step.step.expectedText)
    }

    private fun MixStep.tilesOrEmpty(): List<LetterTile> =
        when (this) {
            is MixStep.DictationPuzzle -> shuffleIntoTiles(step.expectedText)
            is MixStep.Puzzle -> shuffleIntoTiles(step.expectedText)
            else -> emptyList()
        }

    fun onReplayAudio() {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        val text = when (val step = state.step) {
            is MixStep.Dictation -> step.step.expectedText
            is MixStep.DictationPuzzle -> step.step.expectedText
            is MixStep.Pronunciation -> step.step.expectedText
            else -> return
        }
        viewModelScope.launch(dispatchers.io) { speechSynthesizer.speak(text) }
    }

    fun onAnswerChanged(text: String) {
        updateLoaded { if (it.isEditable) it.copy(answerText = text) else it }
    }

    fun onTileSelected(tile: LetterTile) {
        updateLoaded { if (it.isEditable) it.copy(placedTiles = it.placedTiles + tile) else it }
    }

    fun onUndo() {
        updateLoaded { if (it.canUndo) it.copy(placedTiles = it.placedTiles.dropLast(1)) else it }
    }

    fun onOptionSelected(option: String) {
        updateLoaded { if (it.isEditable) it.copy(selectedOption = option) else it }
    }

    fun onTipRequested() {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        if (!state.canUseTip) return
        val hint = when (val step = state.step) {
            is MixStep.Dictation -> step.step.translationText
            is MixStep.DictationPuzzle -> step.step.translationText
            is MixStep.Puzzle -> step.step.clueText
            is MixStep.Pronunciation -> step.step.transcription
            else -> return
        }
        tipsUsedCount++
        updateLoaded { it.copy(tipUsed = true, tipText = hint) }
    }

    fun onRecordRequested() {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        if (!state.canRecord) return
        updateLoaded { it.copy(recordingState = RecordingState.RECORDING, answerText = "") }
        viewModelScope.launch(dispatchers.io) {
            try {
                val result = speechRecognizerService.recognize()
                updateLoaded {
                    it.copy(
                        recordingState = RecordingState.RECORDED,
                        answerText = result.recognizedText,
                        recognizedConfidence = result.confidence,
                    )
                }
            } catch (failure: SpeechRecognitionFailed) {
                updateLoaded { it.copy(recordingState = RecordingState.IDLE) }
            }
        }
    }

    fun onTrueOrFalseAnswer(answeredTrue: Boolean) {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        val step = state.step as? MixStep.TrueOrFalse ?: return
        if (!state.isEditable) return
        updateLoaded { it.copy(isSubmitting = true, answeredTrue = answeredTrue) }
        viewModelScope.launch(dispatchers.io) {
            val response = submitTrueOrFalse(
                SubmitTrueOrFalseAnswerRequest(
                    sessionId = sessionId,
                    stepIndex = step.stepIndex,
                    vocabularyItemId = step.step.vocabularyItemId,
                    isDisplayedTranslationCorrect = step.step.isDisplayedTranslationCorrect,
                    userAnsweredTrue = answeredTrue,
                ),
            )
            finishStep(
                isCorrect = response.outcome == StepOutcome.CORRECT,
                skipped = false,
                revealed = null,
                word = step.step.word,
                translation = step.step.displayedTranslation,
            )
        }
    }

    fun onCheck() {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        if (!state.canCheck) return
        submitStep(state, skipped = false)
    }

    fun onSkip() {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        if (!state.canSkip) return
        submitStep(state, skipped = true)
    }

    private fun submitStep(
        state: MixUiState.Loaded,
        skipped: Boolean,
    ) {
        updateLoaded { it.copy(isSubmitting = true) }
        viewModelScope.launch(dispatchers.io) {
            when (val step = state.step) {
                is MixStep.Dictation -> {
                    val response = submitDictation(
                        SubmitDictationAnswerRequest(
                            sessionId,
                            step.stepIndex,
                            step.step.vocabularyItemId,
                            step.step.expectedText,
                            if (skipped) "" else state.answerText,
                            state.tipUsed,
                            skipped,
                        ),
                    )
                    finishStep(
                        response.outcome == StepOutcome.CORRECT,
                        skipped,
                        response.expectedText,
                        step.step.expectedText,
                        step.step.translationText,
                    )
                }

                is MixStep.DictationPuzzle -> {
                    val response = submitDictationPuzzle(
                        SubmitDictationPuzzleAnswerRequest(
                            sessionId,
                            step.stepIndex,
                            step.step.vocabularyItemId,
                            step.step.expectedText,
                            if (skipped) "" else state.builtAnswer,
                            state.tipUsed,
                            skipped,
                        ),
                    )
                    finishStep(
                        response.outcome == StepOutcome.CORRECT,
                        skipped,
                        response.expectedText,
                        step.step.expectedText,
                        step.step.translationText,
                    )
                }

                is MixStep.Puzzle -> {
                    val response = submitPuzzle(
                        SubmitPuzzleAnswerRequest(
                            sessionId,
                            step.stepIndex,
                            step.step.vocabularyItemId,
                            step.step.expectedText,
                            if (skipped) "" else state.builtAnswer,
                            state.tipUsed,
                            skipped,
                        ),
                    )
                    finishStep(
                        response.outcome == StepOutcome.CORRECT,
                        skipped,
                        response.expectedText,
                        step.step.expectedText,
                        step.step.clueText,
                    )
                }

                is MixStep.ImageTest -> {
                    val response = submitImageTest(
                        SubmitImageTestAnswerRequest(
                            sessionId,
                            step.stepIndex,
                            step.step.vocabularyItemId,
                            step.step.correctOption,
                            if (skipped) null else state.selectedOption,
                            skipped,
                        ),
                    )
                    updateLoaded { it.copy(correctOption = response.correctOption) }
                    finishStep(
                        response.outcome == StepOutcome.CORRECT,
                        skipped,
                        response.correctOption,
                        step.step.correctOption,
                        step.step.clueText,
                    )
                }

                is MixStep.Pronunciation -> {
                    val response = submitPronunciation(
                        SubmitPronunciationResultRequest(
                            sessionId,
                            step.stepIndex,
                            step.step.vocabularyItemId,
                            step.step.expectedText,
                            if (skipped) "" else state.answerText,
                            state.recognizedConfidence,
                            state.tipUsed,
                            skipped,
                        ),
                    )
                    finishStep(
                        response.outcome == StepOutcome.CORRECT,
                        skipped,
                        response.expectedText,
                        step.step.expectedText,
                        step.step.clueText,
                    )
                }

                is MixStep.TrueOrFalse -> Unit
            }
        }
    }

    private suspend fun finishStep(
        isCorrect: Boolean,
        skipped: Boolean,
        revealed: String?,
        word: String,
        translation: String,
    ) {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        val outcome = when {
            skipped -> AnswerState.Skipped(revealed)
            isCorrect -> AnswerState.Correct
            else -> AnswerState.Incorrect(revealed)
        }
        when {
            skipped -> skippedCount++
            isCorrect -> correctCount++
            else -> incorrectCount++
        }
        wordResults += WordResultEntry(word, translation, outcome, state.tipUsed)

        updateLoaded { it.copy(answerState = outcome, isSubmitting = false) }
        if (outcome is AnswerState.Incorrect) return

        delay(if (skipped) SKIPPED_ANSWER_ADVANCE_DELAY_MS else CORRECT_ANSWER_ADVANCE_DELAY_MS)
        advanceToNextStep()
    }

    fun onNext() {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        if (!state.awaitingNext) return
        updateLoaded { it.copy(isSubmitting = true) }
        viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
    }

    private suspend fun advanceToNextStep() {
        val state = _uiState.value as? MixUiState.Loaded ?: return
        val nextIndex = state.stepIndex + 1
        if (nextIndex >= steps.size) {
            lastSessionResultsHolder.wordResults = wordResults.toList()
            _navigationEvents.emit(
                SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount, tipsUsedCount),
            )
            return
        }
        openStep(nextIndex)
    }

    private inline fun updateLoaded(transform: (MixUiState.Loaded) -> MixUiState.Loaded) {
        _uiState.update { current -> if (current is MixUiState.Loaded) transform(current) else current }
    }
}
