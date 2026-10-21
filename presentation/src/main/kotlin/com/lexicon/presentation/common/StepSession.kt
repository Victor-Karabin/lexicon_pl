package com.lexicon.presentation.common

import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class StepSession<T, K>(
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val speechSynthesizer: SpeechSynthesizer,
    private val resultsHolder: LastSessionResultsHolder,
    private val recordStudy: RecordStudyActivityUseCase,
    private val keyOf: (T) -> K,
    private val pick: suspend (asked: Set<K>) -> T?,
) {
    val totalSteps: Int = AppSettings.DEFAULT_STEP_COUNT

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private val asked = mutableSetOf<K>()
    private val results = mutableListOf<WordResultEntry>()
    private var correct = 0
    private var incorrect = 0

    @Volatile
    private var isMoving = false

    fun begin(onFirst: (T?) -> Unit) {
        scope.launch(dispatchers.io) {
            val first = pick(asked)?.also { asked += keyOf(it) }
            onFirst(first)
        }
    }

    fun record(entry: WordResultEntry) {
        if (entry.outcome == AnswerState.Correct) correct++ else incorrect++
        results += entry
    }

    fun advance(
        stepIndex: Int,
        onStep: (item: T, index: Int) -> Unit,
    ) {
        if (isMoving) return
        isMoving = true
        scope.launch(dispatchers.io) {
            val nextIndex = stepIndex + 1
            val item = if (nextIndex < totalSteps) pick(asked) else null

            if (item == null) {
                resultsHolder.wordResults = results.toList()
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correct, incorrect, 0, 0))
                return@launch
            }

            asked += keyOf(item)
            onStep(item, nextIndex)
            isMoving = false
        }
    }

    fun speak(text: String) {
        scope.launch(dispatchers.io) { speechSynthesizer.speakQuietly(text) }
    }

    fun studied() = recordStudy.markIn(scope, dispatchers)
}
