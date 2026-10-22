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
import com.lexicon.presentation.common.StepSession
import com.lexicon.presentation.common.TextResource
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.labelId
import com.lexicon.presentation.course.COURSE_ID_ARG
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
    speechSynthesizer: SpeechSynthesizer,
    lastSessionResultsHolder: LastSessionResultsHolder,
    recordStudy: RecordStudyActivityUseCase,
    getStepCount: GetStepCountUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val courseId: String = savedStateHandle.get<String>(COURSE_ID_ARG).orEmpty()

    private val _uiState = MutableStateFlow(DeclensionUiState())
    val uiState: StateFlow<DeclensionUiState> = _uiState.asStateFlow()

    private val session = StepSession(
        scope = viewModelScope,
        dispatchers = dispatchers,
        speechSynthesizer = speechSynthesizer,
        resultsHolder = lastSessionResultsHolder,
        recordStudy = recordStudy,
        getStepCount = getStepCount,
        keyOf = DeclensionTable::word,
        pick = { asked -> nextQuestion(courseId, asked) },
    )

    val navigationEvents: SharedFlow<SessionNavigationEvent> = session.navigationEvents

    init {
        session.begin { first ->
            val isComplete = first == null && isCourseComplete()
            _uiState.update {
                it.copy(isLoading = false, table = first, totalSteps = session.totalSteps, isComplete = isComplete)
            }
        }
    }

    fun onAnswerChanged(
        case: GrammaticalCase,
        value: String,
    ) {
        session.studied()
        _uiState.update { state ->
            if (state.isAnswered) state else state.copy(answers = (state.answers + (case to value)).toImmutableMap())
        }
    }

    fun onCheck() {
        val state = _uiState.value
        val table = state.table ?: return
        if (!state.canCheck) return
        session.studied()
        _uiState.update { it.copy(isChecking = true) }

        viewModelScope.launch(dispatchers.io) {
            val response = submitAnswer(SubmitDeclensionAnswerRequest(courseId, table, state.answers))

            table.steps.forEach { step ->
                val right = response.correctness[step.variant.case] == true
                val expected = LessonAnswerChecker.closest(step.forms, state.answers[step.variant.case].orEmpty())
                session.record(
                    WordResultEntry(
                        word = expected,
                        translation = table.word,
                        translationText = TextResource(
                            R.string.declension_result_label,
                            listOf(table.word, TextResource(step.variant.column.labelId()), TextResource(step.variant.case.labelId())),
                        ),
                        outcome = if (right) AnswerState.Correct else AnswerState.Incorrect(expected),
                        typed = state.answers[step.variant.case],
                    ),
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
        session.studied()
        session.advance(_uiState.value.stepIndex) { table, index ->
            _uiState.update {
                it.copy(
                    table = table,
                    stepIndex = index,
                    answers = persistentMapOf(),
                    verdicts = persistentMapOf(),
                    answerState = AnswerState.Unanswered,
                )
            }
        }
    }

    private suspend fun isCourseComplete(): Boolean = loadCourses().firstOrNull()?.progress?.isComplete == true

    fun onSpeak(form: String) {
        if (form.isBlank()) return
        session.speak(form)
    }
}
