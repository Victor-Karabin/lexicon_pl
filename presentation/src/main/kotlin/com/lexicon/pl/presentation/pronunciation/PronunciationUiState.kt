package com.lexicon.pl.presentation.pronunciation

import com.lexicon.pl.presentation.common.AnswerState

enum class RecordingState { IDLE, RECORDING, PROCESSING }

data class PronunciationUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val recordingState: RecordingState = RecordingState.IDLE,
    val recognizedText: String? = null,
    val answerState: AnswerState = AnswerState.UNANSWERED,
    val revealedAnswer: String? = null,
    val tipUsed: Boolean = false,
    val isSessionComplete: Boolean = false,
) {
    val isEditable: Boolean get() = answerState == AnswerState.UNANSWERED
    val canRecord: Boolean get() = isEditable && recordingState == RecordingState.IDLE
    val canUseTip: Boolean get() = isEditable && !tipUsed && recordingState == RecordingState.IDLE
    val canSkip: Boolean get() = isEditable && recordingState == RecordingState.IDLE
    val awaitingNext: Boolean get() = answerState == AnswerState.INCORRECT || answerState == AnswerState.SKIPPED
}
