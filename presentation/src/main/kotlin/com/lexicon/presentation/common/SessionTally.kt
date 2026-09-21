package com.lexicon.presentation.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionTally(
    private val lastResults: LastSessionResultsHolder,
) {
    private var correct = 0
    private var incorrect = 0
    private var skipped = 0
    private var tipsUsed = 0
    private val words = mutableListOf<WordResultEntry>()

    private val _events = MutableSharedFlow<SessionNavigationEvent>()
    val events: SharedFlow<SessionNavigationEvent> = _events.asSharedFlow()

    fun countTip() {
        tipsUsed++
    }

    fun record(
        outcome: AnswerState,
        word: String? = null,
        translation: String = "",
        tipUsed: Boolean = false,
    ) {
        when (outcome) {
            AnswerState.Correct -> correct++
            is AnswerState.Incorrect -> incorrect++
            is AnswerState.Skipped -> skipped++
            AnswerState.Unanswered -> return
        }
        if (word != null) words += WordResultEntry(word, translation, outcome, tipUsed)
    }

    suspend fun complete() {
        lastResults.wordResults = words.toList()
        _events.emit(SessionNavigationEvent.SessionComplete(correct, incorrect, skipped, tipsUsed))
    }
}
