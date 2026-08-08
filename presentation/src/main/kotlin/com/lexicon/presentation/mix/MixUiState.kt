package com.lexicon.presentation.mix

import com.lexicon.interactors.mix.MixStep
import com.lexicon.interactors.mix.MixTrainingType
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.revealedAnswer
import com.lexicon.presentation.pronunciation.RecordingState

sealed interface MixUiState {
    data object Loading : MixUiState

    /**
     * One state class covering every generated step type. The fields are a union of what the six
     * trainings need; which ones matter is decided by [step]'s type. Keeping it flat avoids a
     * parallel state hierarchy that would have to be kept in sync with [MixStep].
     */
    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val step: MixStep,
        val answerState: AnswerState = AnswerState.Unanswered,
        /** Dictation: typed answer. Pronunciation: the recognized speech. */
        val answerText: String = "",
        /** Puzzle and Dictation Puzzle. */
        val stepTiles: List<LetterTile> = emptyList(),
        val placedTiles: List<LetterTile> = emptyList(),
        /** Image Test. */
        val selectedOption: String? = null,
        val correctOption: String? = null,
        /** True or False: which button was tapped, so only that one shows the outcome colour. */
        val answeredTrue: Boolean? = null,
        /** Pronunciation Check. */
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

        /**
         * True or False answers on tap, so it has no Check button at all — rendering a permanently
         * disabled one would read as the step being broken. The rest need one.
         */
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

        /** Skip is inherited from the originating training, and True or False has none. */
        val canSkip: Boolean get() = isEditable && trainingType != MixTrainingType.TRUE_OR_FALSE

        /** Tip is inherited from the originating training, so only the ones that offer it get it. */
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

        /** Skip auto-advances; only an incorrect answer waits for an explicit Next. */
        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect && !isSubmitting

        /**
         * Outcome colouring for one of the two True/False buttons: true = correct, false =
         * incorrect, null = leave it in its default colour.
         *
         * Unlike the standalone training, where True is permanently green and False permanently
         * red, both start neutral here and only the button actually tapped takes on a colour — so
         * the colour reports the result rather than labelling the options.
         */
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
