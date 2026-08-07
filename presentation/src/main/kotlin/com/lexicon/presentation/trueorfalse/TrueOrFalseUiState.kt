package com.lexicon.presentation.trueorfalse

/** The user answers as many items as they can before this runs out, rather than a fixed step count. */
const val TRUE_OR_FALSE_TIME_LIMIT_SECONDS = 60

sealed interface TrueOrFalseUiState {
    data object Loading : TrueOrFalseUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val timeRemainingSeconds: Int = 0,
        val word: String = "",
        val displayedTranslation: String = "",
        val isSubmitting: Boolean = false,
        val isSessionComplete: Boolean = false,
    ) : TrueOrFalseUiState {
        val isEditable: Boolean get() = !isSubmitting && !isSessionComplete
    }
}
