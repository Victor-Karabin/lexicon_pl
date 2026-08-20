package com.lexicon.presentation.mix

import com.lexicon.interactors.mix.MixStep
import com.lexicon.interactors.mix.MixTrainingType
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.revealedAnswer
import com.lexicon.presentation.pronunciation.RecordingState

sealed interface MixUiState {
    data object Loading : MixUiState

    data object Unavailable : MixUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val step: MixStep,
        val answerState: AnswerState = AnswerState.Unanswered,
        val answerText: String = "",
        val stepTiles: List<LetterTile> = emptyList(),
        val placedTiles: List<LetterTile> = emptyList(),
        val selectedOption: String? = null,
        val correctOption: String? = null,
        val answeredTrue: Boolean? = null,
        val recordingState: RecordingState = RecordingState.IDLE,
        val recognizedConfidence: Float? = null,
        val tipUsed: Boolean = false,
        val tipText: String? = null,
        val isSubmitting: Boolean = false,
    ) : MixUiState {
        val trainingType: MixTrainingType get() = step.trainingType
        val revealedAnswer: String? get() = answerState.revealedAnswer
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered && !isSubmitting

        val builtAnswer: String get() = placedTiles.joinToString("") { it.char.toString() }
        val availableTiles: List<LetterTile> get() = stepTiles.filterNot { tile -> placedTiles.any { it.id == tile.id } }

        val hasCheckAction: Boolean get() = trainingType != MixTrainingType.TRUE_OR_FALSE

        val canCheck: Boolean
            get() = isEditable && when (trainingType) {
                MixTrainingType.DICTATION -> answerText.isNotBlank()
                MixTrainingType.DICTATION_PUZZLE, MixTrainingType.PUZZLE -> placedTiles.isNotEmpty()
                MixTrainingType.IMAGE_TEST -> selectedOption != null
                MixTrainingType.PRONUNCIATION_CHECK -> recordingState == RecordingState.RECORDED
                MixTrainingType.TRUE_OR_FALSE -> false
            }

        val canUndo: Boolean get() = isEditable && placedTiles.isNotEmpty()

        val canSkip: Boolean get() = isEditable && trainingType != MixTrainingType.TRUE_OR_FALSE

        val canUseTip: Boolean
            get() = isEditable && !tipUsed && trainingType in
                setOf(
                    MixTrainingType.DICTATION,
                    MixTrainingType.DICTATION_PUZZLE,
                    MixTrainingType.PUZZLE,
                    MixTrainingType.PRONUNCIATION_CHECK,
                )

        val canRecord: Boolean
            get() = isEditable && recordingState != RecordingState.RECORDING && recordingState != RecordingState.PROCESSING

        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect && !isSubmitting

        fun trueOrFalseOutcomeFor(isTrueButton: Boolean): Boolean? =
            if (answeredTrue != isTrueButton) {
                null
            } else {
                when (answerState) {
                    is AnswerState.Correct -> true
                    is AnswerState.Incorrect -> false
                    else -> null
                }
            }
    }
}
