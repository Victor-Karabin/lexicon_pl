package com.lexicon.presentation.conjugation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationTableUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerRequest
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.StepSession
import com.lexicon.presentation.common.WordResultEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConjugationUiState(
    val isLoading: Boolean = true,
    val table: ConjugationTable? = null,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answers: ImmutableMap<GrammaticalPerson, String> = persistentMapOf(),
    val verdicts: ImmutableMap<GrammaticalPerson, AnswerVerdict> = persistentMapOf(),
    val answerState: AnswerState = AnswerState.Unanswered,
    val isComplete: Boolean = false,
    val isPickingImage: Boolean = false,
    val imageChoices: ImmutableList<String> = persistentListOf(),
    val isLoadingImages: Boolean = false,
    val hasMoreImages: Boolean = true,
) {
    val isAnswered: Boolean get() = answerState !is AnswerState.Unanswered

    val canCheck: Boolean
        get() = !isAnswered && table?.steps?.all { !answers[it.variant.person].isNullOrBlank() } == true
}

const val CONJUGATION_COURSE_ARG = "courseId"

class ConjugationViewModel(
    savedStateHandle: SavedStateHandle,
    private val nextQuestion: NextConjugationTableUseCase,
    private val submitAnswer: SubmitConjugationAnswerUseCase,
    private val loadImageChoices: LoadVerbImageChoicesUseCase,
    private val chooseImage: ChooseVerbImageUseCase,
    private val loadCourses: LoadConjugationCoursesUseCase,
    speechSynthesizer: SpeechSynthesizer,
    lastSessionResultsHolder: LastSessionResultsHolder,
    recordStudy: RecordStudyActivityUseCase,
    getStepCount: GetStepCountUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val courseId: String = savedStateHandle.get<String>(CONJUGATION_COURSE_ARG).orEmpty()

    private val _uiState = MutableStateFlow(ConjugationUiState())
    val uiState: StateFlow<ConjugationUiState> = _uiState.asStateFlow()

    private val session = StepSession(
        scope = viewModelScope,
        dispatchers = dispatchers,
        speechSynthesizer = speechSynthesizer,
        resultsHolder = lastSessionResultsHolder,
        recordStudy = recordStudy,
        getStepCount = getStepCount,
        keyOf = ConjugationTable::infinitive,
        pick = { asked -> nextQuestion(courseId, asked) },
    )

    val navigationEvents: SharedFlow<SessionNavigationEvent> = session.navigationEvents

    @Volatile
    private var isChecking = false

    init {
        session.begin { first ->
            val isComplete = first == null && isCourseComplete()
            _uiState.update {
                it.copy(isLoading = false, table = first, totalSteps = session.totalSteps, isComplete = isComplete)
            }
        }
    }

    fun onAnswerChanged(
        person: GrammaticalPerson,
        value: String,
    ) {
        session.studied()
        _uiState.update { state ->
            if (state.isAnswered) state else state.copy(answers = (state.answers + (person to value)).toImmutableMap())
        }
    }

    fun onCheck() {
        val state = _uiState.value
        val table = state.table ?: return
        if (!state.canCheck || isChecking) return
        isChecking = true
        session.studied()

        viewModelScope.launch(dispatchers.io) {
            try {
                val response = submitAnswer(SubmitConjugationAnswerRequest(courseId, table, state.answers))
                val allRight = response.allCorrect

                table.steps.forEach { step ->
                    val right = response.correctness[step.variant.person] == true
                    session.record(
                        WordResultEntry(
                            word = step.spokenForm,
                            translation = "${table.infinitive} · ${step.variant.person.label}",
                            outcome = if (right) AnswerState.Correct else AnswerState.Incorrect(step.spokenForm),
                            typed = state.answers[step.variant.person],
                        ),
                    )
                }

                _uiState.update {
                    it.copy(
                        verdicts = response.verdicts.toImmutableMap(),
                        answerState = if (allRight) AnswerState.Correct else AnswerState.Incorrect(""),
                    )
                }
            } finally {
                isChecking = false
            }
        }
    }

    fun onNext() {
        if (!_uiState.value.isAnswered) return
        session.advance(_uiState.value.stepIndex, beforeMove = session::studied) { table, index ->
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

    fun onEditVerb() {
        val table = _uiState.value.table ?: return
        _uiState.update { it.copy(isPickingImage = true, isLoadingImages = true, hasMoreImages = true) }
        viewModelScope.launch(dispatchers.io) {
            val choices = loadImageChoices(table.infinitive, table.translation)
            _uiState.update { it.copy(imageChoices = choices, isLoadingImages = false, hasMoreImages = choices.isNotEmpty()) }
        }
    }

    fun onMoreVerbImages() {
        val state = _uiState.value
        val table = state.table ?: return
        if (state.isLoadingImages || !state.hasMoreImages) return

        _uiState.update { it.copy(isLoadingImages = true) }
        viewModelScope.launch(dispatchers.io) {
            val more = loadImageChoices(table.infinitive, table.translation, skip = state.imageChoices.size)
            _uiState.update {
                it.copy(
                    imageChoices = (it.imageChoices + more).distinct().toImmutableList(),
                    isLoadingImages = false,
                    hasMoreImages = more.isNotEmpty(),
                )
            }
        }
    }

    fun onImageChosen(url: String) {
        val table = _uiState.value.table ?: return
        _uiState.update {
            it.copy(isPickingImage = false, imageChoices = persistentListOf(), table = it.table?.copy(imageUrl = url))
        }
        viewModelScope.launch(dispatchers.io) { chooseImage(table.infinitive, table.translation, url) }
    }

    fun onImagePickerDismissed() = _uiState.update { it.copy(isPickingImage = false, imageChoices = persistentListOf()) }
}
