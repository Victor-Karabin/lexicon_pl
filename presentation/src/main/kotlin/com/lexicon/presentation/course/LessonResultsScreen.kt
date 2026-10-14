package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.GetLastLessonResultsUseCase
import com.lexicon.interactors.course.GetLessonScriptUseCase
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.LessonResults
import com.lexicon.model.course.GrammarRule
import com.lexicon.model.course.LessonId
import com.lexicon.presentation.R
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class LessonResultsUiState(
    val isLoading: Boolean = true,
    val lessonNumber: Int? = null,
    val lessonTitle: String? = null,
    val results: LessonResults? = null,
    val rules: ImmutableList<GrammarRule> = persistentListOf(),
    val openRule: Int? = null,
    val isGrammarShown: Boolean = false,
)

class LessonResultsViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLastLessonResults: GetLastLessonResultsUseCase,
    private val getLesson: GetLessonUseCase,
    private val getLessonScript: GetLessonScriptUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val lessonId = LessonId(savedStateHandle.get<String>(LESSON_ID_ARG).orEmpty())

    private val _uiState = MutableStateFlow(LessonResultsUiState())
    val uiState: StateFlow<LessonResultsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val lesson = getLesson(lessonId)
            _uiState.value = LessonResultsUiState(
                isLoading = false,
                lessonNumber = lesson?.number,
                lessonTitle = lesson?.title,
                results = getLastLessonResults(lessonId),
                rules = getLessonScript(lessonId)?.rules.orEmpty().toImmutableList(),
            )
        }
    }

    fun onRuleShown(rule: Int?) = _uiState.update { it.copy(isGrammarShown = true, openRule = rule) }

    fun onGrammarDismissed() = _uiState.update { it.copy(isGrammarShown = false, openRule = null) }
}

@Composable
fun LessonResultsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonResultsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = uiState.lessonNumber?.let { stringResource(R.string.lesson_results_title, it) } ?: stringResource(R.string.lesson_results)

    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
    ) { padding ->
        val results = uiState.results
        when {
            uiState.isLoading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            results == null ->
                Box(Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.lesson_results_none), style = MaterialTheme.typography.bodyMedium)
                }

            else ->
                LessonResultsView(
                    results = results,
                    title = uiState.lessonTitle,
                    onRuleOpened = viewModel::onRuleShown,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.spacingMedium),
                )
        }
    }

    if (uiState.isGrammarShown) {
        GrammarSheet(
            rules = uiState.rules,
            openRule = uiState.openRule,
            onRuleShown = viewModel::onRuleShown,
            onDismiss = viewModel::onGrammarDismissed,
        )
    }
}
