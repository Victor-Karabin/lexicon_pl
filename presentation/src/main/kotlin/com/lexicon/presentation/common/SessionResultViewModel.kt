package com.lexicon.presentation.common

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionResultViewModel
    @Inject
    constructor(
        lastSessionResultsHolder: LastSessionResultsHolder,
    ) : ViewModel() {
        val wordResults: List<WordResultEntry> = lastSessionResultsHolder.wordResults
    }
