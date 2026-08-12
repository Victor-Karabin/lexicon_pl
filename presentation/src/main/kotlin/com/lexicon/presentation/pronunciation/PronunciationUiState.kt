package com.lexicon.presentation.pronunciation

import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.revealedAnswer

enum class RecordingState { IDLE, RECORDING, PROCESSING, RECORDED }

enum class RecognitionErrorType { UNAVAILABLE, FAILED }

const val MAX_TIP_LEVEL = 2

sealed interface PronunciationUiState {
    data object Loading : PronunciationUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val word: String = "",
        val recordingState: RecordingState = RecordingState.IDLE,
        val recognizedText: String? = null,
        val recognizedConfidence: Float? = null,
        val recordedAudioPath: String? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        val tipLevel: Int = 0,
        val tipTranslation: String? = null,
        val tipTranscription: String? = null,
        val isSessionComplete: Boolean = false,
        val isSubmitting: Boolean = false,
        val recognitionError: RecognitionErrorType? = null,
    ) : PronunciationUiState {
        val revealedAnswer: String? get() = answerState.revealedAnswer
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered
        val isBusyRecording: Boolean get() = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PROCESSING
        val tipUsed: Boolean get() = tipLevel > 0

        val canRecord: Boolean get() = isEditable && !isSubmitting && !isBusyRecording
        val canPlayRecording: Boolean get() = recordedAudioPath != null && !isBusyRecording
        val canCheck: Boolean get() = isEditable && recordingState == RecordingState.RECORDED && !isSubmitting
        val canUseTip: Boolean get() = isEditable && !isSubmitting && tipLevel < MAX_TIP_LEVEL
        val canSkip: Boolean get() = isEditable && !isSubmitting

        // Skip auto-advances, same as Correct; only Incorrect waits for a manual Next.
        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect && !isSubmitting
    }
}
