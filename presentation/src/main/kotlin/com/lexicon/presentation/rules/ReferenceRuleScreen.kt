package com.lexicon.presentation.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.rules.GetReferenceRuleUseCase
import com.lexicon.model.course.LessonTable
import com.lexicon.model.rules.CASES_RULE
import com.lexicon.model.rules.ReferenceRule
import com.lexicon.model.rules.ReferenceRuleId
import com.lexicon.model.rules.ReferenceSection
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.course.LessonTableView
import com.lexicon.presentation.course.richText
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

const val RULE_ID_ARG = "ruleId"

sealed interface ReferenceRuleUiState {
    data object Loading : ReferenceRuleUiState

    data object NotFound : ReferenceRuleUiState

    data class Loaded(val rule: ReferenceRule) : ReferenceRuleUiState
}

class ReferenceRuleViewModel(
    savedStateHandle: SavedStateHandle,
    getRule: GetReferenceRuleUseCase,
    dispatchers: DispatcherProvider,
) : ViewModel() {
    private val ruleId = ReferenceRuleId(savedStateHandle.get<String>(RULE_ID_ARG).orEmpty())

    private val _uiState = MutableStateFlow<ReferenceRuleUiState>(ReferenceRuleUiState.Loading)
    val uiState: StateFlow<ReferenceRuleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            _uiState.value = getRule(ruleId)?.let { ReferenceRuleUiState.Loaded(it) } ?: ReferenceRuleUiState.NotFound
        }
    }
}

@Composable
fun ReferenceRuleScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReferenceRuleViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReferenceRuleContent(uiState = uiState, onClose = onClose, modifier = modifier)
}

@Composable
private fun ReferenceRuleContent(
    uiState: ReferenceRuleUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = (uiState as? ReferenceRuleUiState.Loaded)?.rule?.title ?: stringResource(R.string.rules_title)
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
    ) { padding ->
        when (uiState) {
            ReferenceRuleUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            ReferenceRuleUiState.NotFound ->
                Box(Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.rules_not_found), style = MaterialTheme.typography.bodyMedium)
                }

            is ReferenceRuleUiState.Loaded ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.spacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge),
                ) {
                    uiState.rule.sections.forEach { ReferenceSectionView(it) }
                }
        }
    }
}

@Composable
private fun ReferenceSectionView(section: ReferenceSection) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        Text(richText(section.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        section.intro.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
        section.tables.forEach { LessonTableView(it) }
        section.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

private val previewRule = ReferenceRule(
    id = CASES_RULE,
    title = "Przypadki",
    sections = persistentListOf(
        ReferenceSection(
            title = "Noun endings: singular",
            intro = persistentListOf(),
            tables = persistentListOf(
                LessonTable(
                    header = persistentListOf("Case", "Masc. things", "Fem."),
                    rows = persistentListOf(
                        persistentListOf("**M.**", "dom", "kobiet**a**"),
                        persistentListOf("**D.**", "dom**u**", "kobiet**y**"),
                    ),
                ),
            ),
            notes = persistentListOf("**Feminine:** dative = locative (*kobiecie, mapie, ulicy*)."),
        ),
    ),
)

@LightDarkPreview
@Composable
private fun ReferenceRulePreview() {
    LexiconTheme {
        ReferenceRuleContent(uiState = ReferenceRuleUiState.Loaded(previewRule), onClose = {})
    }
}
