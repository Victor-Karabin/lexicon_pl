package com.lexicon.presentation.pronunciation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.audio.AudioPlayer
import com.lexicon.android.recognition.SpeechRecognitionFailed
import com.lexicon.android.recognition.SpeechRecognizerService
import com.lexicon.android.speech.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSentencesUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.training.StepOutcome
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.trainingVocabularyIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val CORRECT_ANSWER_ADVANCE_DELAY_MS = 400L
private const val SKIPPED_ANSWER_ADVANCE_DELAY_MS = 700L

const val PRONUNCIATION_SENTENCES_ARG = "sentences"

class PronunciationViewModel(
    savedStateHandle: SavedStateHandle,
    private val startSessionUseCase: StartPronunciationSessionUseCase,
    private val startSentencesUseCase: StartPronunciationSentencesUseCase,
    private val submitResultUseCase: SubmitPronunciationResultUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val speechRecognizerService: SpeechRecognizerService,
    private val audioPlayer: AudioPlayer,
    private val dispatchers: DispatcherProvider,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
) : ViewModel() {
    private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

    /** Whether this run reads generated sentences rather than single words. */
    private val readsSentences: Boolean = savedStateHandle.get<String>(PRONUNCIATION_SENTENCES_ARG).toBoolean()

    private val _uiState = MutableStateFlow<PronunciationUiState>(PronunciationUiState.Loading)
    val uiState: StateFlow<PronunciationUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private lateinit var sessionId: String
    private var steps: List<PronunciationStepResponse> = emptyList()
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
            val response = when {
                readsSentences -> when (val result = startSentencesUseCase()) {
                    is PronunciationSentencesResult.Ready -> result.session

                    PronunciationSentencesResult.NoFavourites ->
                        return@launch showUnavailable(UnavailableReason.NO_FAVOURITES)

                    PronunciationSentencesResult.Offline ->
                        return@launch showUnavailable(UnavailableReason.OFFLINE)

                    is PronunciationSentencesResult.Refused ->
                        return@launch showUnavailable(UnavailableReason.REFUSED)
                }

                else -> startSessionUseCase(StartPronunciationSessionRequest(vocabularyIds = vocabularyIds))
            }

            sessionId = response.sessionId
            steps = response.steps
            _uiState.update {
                PronunciationUiState.Loaded(
                    stepIndex = 0,
                    totalSteps = steps.size,
                    word = steps.getOrNull(0)?.clueText.orEmpty(),
                    tipsAvailable = !readsSentences,
                )
            }
        }
    }

    private fun showUnavailable(reason: UnavailableReason) {
        _uiState.update { PronunciationUiState.Unavailable(reason) }
    }

    /**
     * Speaks the answer, once it is no longer an answer.
     *
     * This used to play at the start of every step, which handed over the very thing the
     * learner was being asked to produce. It belongs after the attempt, where hearing it
     * is a correction rather than a giveaway.
     */
    private suspend fun speakReferenceAudio() {
        val step = currentStepOrNull() ?: return
        runCatching { speechSynthesizer.speak(step.expectedText) }
    }

    fun onTipRequested() {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        if (!state.canUseTip) return
        val step = currentStepOrNull() ?: return
        if (state.tipLevel == 0) {
            tipsUsedCount++
            updateLoaded { it.copy(tipLevel = 1, tipTranslation = step.expectedText) }
        } else {
            updateLoaded { it.copy(tipLevel = 2, tipTranscription = step.transcription) }
        }
    }

    fun onRecordRequested() {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        if (!state.canRecord) return
        deleteCachedRecording(state.recordedAudioPath)
        updateLoaded {
            it.copy(
                recordingState = RecordingState.RECORDING,
                recognizedText = null,
                recordedAudioPath = null,
                recognitionError = null,
            )
        }
        viewModelScope.launch(dispatchers.io) {
            try {
                val result = speechRecognizerService.recognize()
                updateLoaded {
                    it.copy(
                        recordingState = RecordingState.RECORDED,
                        recognizedText = result.recognizedText,
                        recognizedConfidence = result.confidence,
                        recordedAudioPath = result.audioFilePath,
                    )
                }
            } catch (failure: SpeechRecognitionFailed) {
                val errorType = if (failure.message?.contains("not available") == true) {
                    RecognitionErrorType.UNAVAILABLE
                } else {
                    RecognitionErrorType.FAILED
                }
                updateLoaded { it.copy(recordingState = RecordingState.IDLE, recognitionError = errorType) }
            }
        }
    }

    fun onPlayRecording() {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        val path = state.recordedAudioPath ?: return
        if (!state.canPlayRecording) return
        viewModelScope.launch(dispatchers.io) {
            runCatching { audioPlayer.play(path) }
        }
    }

    fun onCheck() {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        if (!state.canCheck) return
        submitCurrentStep(recognizedText = state.recognizedText.orEmpty(), confidence = state.recognizedConfidence, skipped = false)
    }

    fun onSkip() {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        if (!state.canSkip) return
        submitCurrentStep(recognizedText = "", confidence = null, skipped = true)
    }

    private fun submitCurrentStep(
        recognizedText: String,
        confidence: Float?,
        skipped: Boolean,
    ) {
        val step = currentStepOrNull() ?: return
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        updateLoaded { it.copy(isSubmitting = true) }
        viewModelScope.launch(dispatchers.io) {
            val response =
                submitResultUseCase(
                    SubmitPronunciationResultRequest(
                        sessionId = sessionId,
                        stepIndex = step.stepIndex,
                        vocabularyItemId = step.vocabularyItemId,
                        expectedText = step.expectedText,
                        recognizedText = recognizedText,
                        confidence = confidence,
                        tipUsed = state.tipUsed,
                        skipped = skipped,
                    ),
                )
            applyOutcome(response.outcome, response.expectedText, state.tipUsed)
        }
    }

    private suspend fun applyOutcome(
        outcome: StepOutcome,
        expectedText: String,
        tipUsed: Boolean,
    ) {
        val step = currentStepOrNull()
        when (outcome) {
            StepOutcome.CORRECT -> {
                correctCount++
                step?.let { wordResults += WordResultEntry(it.expectedText, it.clueText, AnswerState.Correct, tipUsed) }
                updateLoaded { it.copy(answerState = AnswerState.Correct) }
                speakReferenceAudio()
                delay(CORRECT_ANSWER_ADVANCE_DELAY_MS)
                advanceToNextStep()
            }
            StepOutcome.INCORRECT -> {
                incorrectCount++
                step?.let {
                    wordResults += WordResultEntry(it.expectedText, it.clueText, AnswerState.Incorrect(expectedText), tipUsed)
                }
                updateLoaded { it.copy(answerState = AnswerState.Incorrect(expectedText), isSubmitting = false) }
                speakReferenceAudio()
            }
            StepOutcome.SKIPPED -> {
                skippedCount++
                step?.let {
                    wordResults += WordResultEntry(it.expectedText, it.clueText, AnswerState.Skipped(expectedText), tipUsed)
                }
                updateLoaded { it.copy(answerState = AnswerState.Skipped(expectedText), isSubmitting = false) }
                speakReferenceAudio()
                delay(SKIPPED_ANSWER_ADVANCE_DELAY_MS)
                advanceToNextStep()
            }
        }
    }

    fun onNext() {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        if (!state.awaitingNext) return
        updateLoaded { it.copy(isSubmitting = true) }
        viewModelScope.launch(dispatchers.io) { advanceToNextStep() }
    }

    private suspend fun advanceToNextStep() {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return
        deleteCachedRecording(state.recordedAudioPath)
        val nextIndex = state.stepIndex + 1
        if (nextIndex >= steps.size) {
            updateLoaded { it.copy(isSessionComplete = true, isSubmitting = false) }
            lastSessionResultsHolder.wordResults = wordResults.toList()
            _navigationEvents.emit(
                SessionNavigationEvent.SessionComplete(correctCount, incorrectCount, skippedCount, tipsUsedCount),
            )
            return
        }
        _uiState.update {
            PronunciationUiState.Loaded(
                stepIndex = nextIndex,
                totalSteps = steps.size,
                word = steps[nextIndex].clueText,
                tipsAvailable = !readsSentences,
            )
        }
    }

    private fun currentStepOrNull(): PronunciationStepResponse? {
        val state = _uiState.value as? PronunciationUiState.Loaded ?: return null
        return steps.getOrNull(state.stepIndex)
    }

    private fun deleteCachedRecording(path: String?) {
        path?.let { runCatching { File(it).delete() } }
    }

    private inline fun updateLoaded(transform: (PronunciationUiState.Loaded) -> PronunciationUiState.Loaded) {
        _uiState.update { current -> if (current is PronunciationUiState.Loaded) transform(current) else current }
    }
}
