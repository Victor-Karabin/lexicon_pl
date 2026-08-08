package com.lexicon.presentation.common

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastSessionResultsHolder
    @Inject
    constructor() {
        var wordResults: List<WordResultEntry> = emptyList()
    }
