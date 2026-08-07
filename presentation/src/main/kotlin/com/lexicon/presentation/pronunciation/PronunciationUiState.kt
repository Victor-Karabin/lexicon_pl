package com.lexicon.presentation.pronunciation

import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.revealedAnswer

enum class RecordingState { IDLE, RECORDING, PROCESSING }

sealed interface PronunciationUiState {
    data object Loading : PronunciationUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val word: String = "",
        val recordingState: RecordingState = RecordingState.IDLE,
        val recognizedText: String? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        /** IPA transcription, shown once Tip is used, before validation. */
        val tipTranscription: String? = null,
        val tipUsed: Boolean = false,
        val isSessionComplete: Boolean = false,
    ) : PronunciationUiState {
        /** Correct answer, shown once the step is validated as Incorrect/Skipped. */
        val revealedAnswer: String? get() = answerState.revealedAnswer
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered
        val canRecord: Boolean get() = isEditable && recordingState == RecordingState.IDLE
        val canUseTip: Boolean get() = isEditable && !tipUsed && recordingState == RecordingState.IDLE
        val canSkip: Boolean get() = isEditable && recordingState == RecordingState.IDLE
        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect || answerState is AnswerState.Skipped
    }
}
