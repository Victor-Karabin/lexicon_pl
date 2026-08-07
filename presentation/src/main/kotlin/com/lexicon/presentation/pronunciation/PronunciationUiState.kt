package com.lexicon.presentation.pronunciation

import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.revealedAnswer

enum class RecordingState { IDLE, RECORDING, PROCESSING, RECORDED }

/** Tip has two levels: 1 reveals the target-language word, 2 additionally reveals its IPA transcription. */
const val MAX_TIP_LEVEL = 2

sealed interface PronunciationUiState {
    data object Loading : PronunciationUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        /** Base-language prompt (e.g. "apple"); the user must pronounce it in the target language ("jabłko"). */
        val word: String = "",
        val recordingState: RecordingState = RecordingState.IDLE,
        val recognizedText: String? = null,
        val recognizedConfidence: Float? = null,
        /** Cached recording of the user's own attempt, playable via [canPlayRecording]. Replaced on each re-record. */
        val recordedAudioPath: String? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        /** 0 = unused, 1 = target-language word revealed, 2 = word + IPA revealed. */
        val tipLevel: Int = 0,
        /** Target-language word to pronounce, revealed at tip level 1. */
        val tipTranslation: String? = null,
        /** IPA transcription of the target-language word, revealed at tip level 2. */
        val tipTranscription: String? = null,
        val isSessionComplete: Boolean = false,
        /** True while Check/Skip/Next is mid-flight — disables the action row so a rapid double-tap can't fire twice. */
        val isSubmitting: Boolean = false,
    ) : PronunciationUiState {
        /** Correct answer, shown once the step is validated as Incorrect/Skipped. */
        val revealedAnswer: String? get() = answerState.revealedAnswer
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered
        val isBusyRecording: Boolean get() = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PROCESSING
        val tipUsed: Boolean get() = tipLevel > 0

        /** Recording (or re-recording, discarding any prior attempt) is allowed any time before Check. */
        val canRecord: Boolean get() = isEditable && !isSubmitting && !isBusyRecording
        val canPlayRecording: Boolean get() = recordedAudioPath != null && !isBusyRecording
        val canCheck: Boolean get() = isEditable && recordingState == RecordingState.RECORDED && !isSubmitting
        val canUseTip: Boolean get() = isEditable && !isSubmitting && tipLevel < MAX_TIP_LEVEL
        val canSkip: Boolean get() = isEditable && !isSubmitting
        val awaitingNext: Boolean get() = (answerState is AnswerState.Incorrect || answerState is AnswerState.Skipped) && !isSubmitting
    }
}
