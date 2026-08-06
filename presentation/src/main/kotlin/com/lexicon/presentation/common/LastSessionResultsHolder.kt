package com.lexicon.presentation.common

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory bridge for the just-completed session's per-word results.
 * A word-level list is too rich to encode into a nav route, so the finishing training ViewModel
 * stashes it here right before navigating, and the Results screen reads it back on arrival.
 */
@Singleton
class LastSessionResultsHolder
    @Inject
    constructor() {
        var wordResults: List<WordResultEntry> = emptyList()
    }
