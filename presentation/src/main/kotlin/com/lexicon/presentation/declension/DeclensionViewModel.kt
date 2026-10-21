package com.lexicon.presentation.declension

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.declension.DeclensionTable
import com.lexicon.interactors.declension.LoadDeclensionCoursesUseCase
import com.lexicon.interactors.declension.NextDeclensionTableUseCase
import com.lexicon.interactors.declension.SubmitDeclensionAnswerRequest
import com.lexicon.interactors.declension.SubmitDeclensionAnswerUseCase
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TextResource
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.labelId
import com.lexicon.presentation.common.markIn
import com.lexicon.presentation.common.speakQuietly
import com.lexicon.presentation.course.COURSE_ID_ARG
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeclensionUiState(
    val isLoading: Boolean = true,
    val table: DeclensionTable? = null,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answers: ImmutableMap<GrammaticalCase, String> = persistentMapOf(),
    val verdicts: ImmutableMap<GrammaticalCase, AnswerVerdict> = persistentMapOf(),
    val answerState: AnswerState = AnswerState.Unanswered,
    val isChecking: Boolean = false,
    val isComplete: Boolean = false,
) {
    val isAnswered: Boolean get() = answerState !is AnswerState.Unanswered

    val canCheck: Boolean
        get() = !isAnswered && !isChecking && table?.steps?.all { !answers[it.variant.case].isNullOrBlank() } == true
}

class DeclensionViewModel(
    savedStateHandle: SavedStateHandle,
    private val nextQuestion: NextDeclensionTableUseCase,
    private val submitAnswer: SubmitDeclensionAnswerUseCase,
    private val loadCourses: LoadDeclensionCoursesUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
    private val recordStudy: RecordStudyActivityUseCase,
    private val getStepCount: GetStepCountUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val courseId: String = savedStateHandle.get<String>(COURSE_ID_ARG).orEmpty()

    private val _uiState = MutableStateFlow(DeclensionUiState())
    val uiState: StateFlow<DeclensionUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private val results = mutableListOf<WordResultEntry>()
    private var correct = 0
    private var incorrect = 0

    init {
        viewModelScope.launch(dispatchers.io) {
            val first = nextQuestion(courseId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    table = first,
                    totalSteps = getStepCount(),
                    isComplete = first == null && isCourseComplete(),
                )
            }
        }
    }

    fun onAnswerChanged(
        case: GrammaticalCase,
        value: String,
    ) {
        studied()
        _uiState.update { state ->
            if (state.isAnswered) state else state.copy(answers = (state.answers + (case to value)).toImmutableMap())
        }
    }

    fun onCheck() {
        val state = _uiState.value
        val table = state.table ?: return
        if (!state.canCheck) return
        studied()
        _uiState.update { it.copy(isChecking = true) }

        viewModelScope.launch(dispatchers.io) {
            val response = submitAnswer(SubmitDeclensionAnswerRequest(courseId, table, state.answers))

            table.steps.forEach { step ->
                val right = response.correctness[step.variant.case] == true
                val expected = LessonAnswerChecker.closest(step.forms, state.answers[step.variant.case].orEmpty())
                if (right) correct++ else incorrect++
                results += WordResultEntry(
                    word = expected,
                    translation = table.word,
                    translationText = TextResource(
                        R.string.declension_result_label,
                        listOf(table.word, TextResource(step.variant.column.labelId()), TextResource(step.variant.case.labelId())),
                    ),
                    outcome = if (right) AnswerState.Correct else AnswerState.Incorrect(expected),
                    typed = state.answers[step.variant.case],
                )
            }

            _uiState.update {
                it.copy(
                    verdicts = response.verdicts.toImmutableMap(),
                    answerState = if (response.allCorrect) AnswerState.Correct else AnswerState.Incorrect(""),
                    isChecking = false,
                )
            }
        }
    }

    fun onNext() {
        studied()
        viewModelScope.launch(dispatchers.io) {
            val state = _uiState.value
            val nextIndex = state.stepIndex + 1
            val table = if (nextIndex >= state.totalSteps) null else nextQuestion(courseId)

            if (table == null) {
                lastSessionResultsHolder.wordResults = results.toList()
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correct, incorrect, 0, 0))
                return@launch
            }

            _uiState.update {
                it.copy(
                    table = table,
                    stepIndex = nextIndex,
                    answers = persistentMapOf(),
                    verdicts = persistentMapOf(),
                    answerState = AnswerState.Unanswered,
                )
            }
        }
    }

    private fun studied() = recordStudy.markIn(viewModelScope, dispatchers)

    private suspend fun isCourseComplete(): Boolean = loadCourses().firstOrNull()?.progress?.isComplete == true

    fun onSpeak(form: String) {
        if (form.isBlank()) return
        viewModelScope.launch(dispatchers.io) { speechSynthesizer.speakQuietly(form) }
    }
}
