package com.lexicon.presentation.common

import androidx.lifecycle.ViewModel

class SessionResultViewModel(
    lastSessionResultsHolder: LastSessionResultsHolder,
) : ViewModel() {
    val wordResults: List<WordResultEntry> = lastSessionResultsHolder.wordResults
}
