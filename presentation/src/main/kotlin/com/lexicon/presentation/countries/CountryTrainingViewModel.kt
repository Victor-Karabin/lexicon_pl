package com.lexicon.presentation.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.CountryField
import com.lexicon.interactors.countries.NextCountryUseCase
import com.lexicon.interactors.countries.SubmitCountryAnswersUseCase
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.StepSession
import com.lexicon.presentation.common.TextResource
import com.lexicon.presentation.common.WordResultEntry
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountryTrainingUiState(
    val isLoading: Boolean = true,
    val country: Country? = null,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answers: ImmutableMap<CountryField, String> = persistentMapOf(),
    val verdicts: ImmutableMap<CountryField, AnswerVerdict> = persistentMapOf(),
    val isChecking: Boolean = false,
) {
    val isAnswered: Boolean get() = verdicts.isNotEmpty()

    val canCheck: Boolean
        get() = !isAnswered && !isChecking && country?.fields?.all { !answers[it].isNullOrBlank() } == true
}

class CountryTrainingViewModel(
    nextCountry: NextCountryUseCase,
    private val submitAnswers: SubmitCountryAnswersUseCase,
    speechSynthesizer: SpeechSynthesizer,
    lastSessionResultsHolder: LastSessionResultsHolder,
    recordStudy: RecordStudyActivityUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CountryTrainingUiState())
    val uiState: StateFlow<CountryTrainingUiState> = _uiState.asStateFlow()

    private val session = StepSession(
        scope = viewModelScope,
        dispatchers = dispatchers,
        speechSynthesizer = speechSynthesizer,
        resultsHolder = lastSessionResultsHolder,
        recordStudy = recordStudy,
        keyOf = Country::code,
        pick = { asked -> nextCountry(asked) },
    )

    val navigationEvents: SharedFlow<SessionNavigationEvent> = session.navigationEvents

    init {
        session.begin { first ->
            _uiState.update { it.copy(isLoading = false, country = first, totalSteps = session.totalSteps) }
        }
    }

    fun onAnswerChanged(
        field: CountryField,
        value: String,
    ) {
        session.studied()
        _uiState.update {
            if (it.isAnswered || it.isChecking) it else it.copy(answers = (it.answers + (field to value)).toImmutableMap())
        }
    }

    fun onCheck() {
        val state = _uiState.value
        val country = state.country ?: return
        if (!state.canCheck) return
        session.studied()
        _uiState.update { it.copy(isChecking = true) }

        viewModelScope.launch(dispatchers.io) {
            val verdicts = submitAnswers(country, state.answers)
            country.fields.forEach { field ->
                val typed = state.answers[field].orEmpty()
                val expected = LessonAnswerChecker.closest(country.answers(field), typed)
                session.record(
                    WordResultEntry(
                        word = expected,
                        translation = country.english,
                        translationText = TextResource(R.string.dot_separated_pair, listOf(country.english, TextResource(field.labelId()))),
                        outcome = if (verdicts[field] == AnswerVerdict.CORRECT) AnswerState.Correct else AnswerState.Incorrect(expected),
                        typed = typed,
                    ),
                )
            }
            _uiState.update { it.copy(verdicts = verdicts, isChecking = false) }
        }
    }

    fun onNext() {
        session.studied()
        session.advance(_uiState.value.stepIndex) { country, index ->
            _uiState.update {
                it.copy(country = country, stepIndex = index, answers = persistentMapOf(), verdicts = persistentMapOf())
            }
        }
    }

    fun onSpeak(form: String) = session.speak(form)
}
