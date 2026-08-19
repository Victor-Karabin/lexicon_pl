package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetStudySetState
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.resolve
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.minutes

@Composable
fun PresetDetailScreen(
    onClose: () -> Unit,
    onEditWord: (VocabularyId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val changePresets by viewModel.changePresetsState.collectAsState()

    val selection = rememberWordSelection()

    val snackbarHostState = remember { SnackbarHostState() }
    val deleted = (uiState as? PresetDetailUiState.Loaded)?.lastDeleted
    val deletedMessage = when (deleted) {
        null -> null
        is DeletedItem.Words -> pluralStringResource(R.plurals.vocabulary_deleted_words, deleted.ids.size, deleted.ids.size)
        else -> stringResource(R.string.vocabulary_deleted, deleted.label)
    }
    val undoLabel = stringResource(R.string.vocabulary_undo)

    LaunchedEffect(deleted) {
        if (deletedMessage == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete() else viewModel.onDeleteMessageShown()
    }

    PresetDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onClose = onClose,
        onWordStudySetToggled = viewModel::onWordStudySetToggled,
        onPronounceWord = viewModel::onPronounceWord,
        onPresetStudySetToggled = viewModel::onPresetStudySetToggled,
        onWordDeleted = viewModel::onWordDeleted,
        onChangePresets = viewModel::onChangePresetsRequested,
        onEditWord = onEditWord,
        selection = selection,
        onDeleteSelected = {
            viewModel.onSelectedWordsDeleted(selection.selected)
            selection.clear()
        },
        modifier = modifier,
    )

    changePresets?.let { state ->
        ChangePresetsSheet(
            state = state,
            onToggle = viewModel::onPresetMembershipToggled,
            onDismiss = viewModel::onChangePresetsDismissed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDetailContent(
    uiState: PresetDetailUiState,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onWordStudySetToggled: (VocabularyId, Boolean) -> Unit,
    onPronounceWord: (Word) -> Unit,
    onPresetStudySetToggled: (PresetStudySetState) -> Unit,
    onWordDeleted: (Word) -> Unit,
    onChangePresets: (Word) -> Unit,
    onEditWord: (VocabularyId) -> Unit,
    selection: WordSelection,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is PresetDetailUiState.Loaded -> uiState.preset.title.resolve(uiState.languageTag)
        else -> stringResource(R.string.preset_detail_title)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState) {
            is PresetDetailUiState.Loading ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

            is PresetDetailUiState.NotFound ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.preset_detail_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

            is PresetDetailUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (selection.isActive) {
                        WordSelectionBar(
                            count = selection.count,
                            onDelete = onDeleteSelected,
                            onCancel = selection::clear,
                        )
                    }
                    PresetHeader(uiState, onPresetStudySetToggled)
                    HorizontalDivider()

                    if (uiState.isLoadingWords) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = Dimens.spacingSmall)) {
                            wordRows(
                                words = uiState.words,
                                onStudySetToggled = onWordStudySetToggled,
                                onPronounce = onPronounceWord,
                                onChangePresets = onChangePresets,
                                onDelete = onWordDeleted,
                                onEdit = { onEditWord(it.id) },
                                selection = selection,
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun PresetHeader(
    uiState: PresetDetailUiState.Loaded,
    onStudySetToggled: (PresetStudySetState) -> Unit,
) {
    val skin = presetTileSkin(uiState.preset)
    GradientTile(skin = skin) {
        PresetSummary(
            preset = uiState.preset,
            languageTag = uiState.languageTag,
            studySetState = uiState.studySetState,
            skin = skin,
            onStudySetToggled = { onStudySetToggled(uiState.studySetState) },
            showTitle = false,
        )
    }
}

private val previewPreset = VocabularyPreset(
    id = PresetId("food"),
    title = LocalizedText(mapOf("en" to "Food")),
    description = LocalizedText(mapOf("en" to "Meals, ingredients, fruit and vegetables.")),
    category = PresetCategory("everyday-life", 3, LocalizedText(mapOf("en" to "Everyday life"))),
    icon = "restaurant",
    color = "#EF6C00",
    popularity = 17,
    estimatedDuration = 58.minutes,
    vocabularyIds = List(58) { VocabularyId(it.toLong()) }.toImmutableList(),
)

@LightDarkPreview
@Composable
private fun PresetDetailPreview() {
    LexiconTheme {
        PresetDetailContent(
            uiState = PresetDetailUiState.Loaded(
                preset = previewPreset,
                words = persistentListOf(
                    Word(VocabularyId(1), "chleb", "bread", "xlɛp", isInStudySet = true),
                    Word(VocabularyId(2), "jabłko", "apple", "ˈjabwkɔ"),
                    Word(VocabularyId(3), "mleko", "milk", "ˈmlɛkɔ", isInStudySet = true),
                    Word(VocabularyId(4), "ziemniak", "potato", "ˈʑɛmɲak"),
                ),
                studySetState = PresetStudySetState.SOME,
                isLoadingWords = false,
            ),
            onClose = {},
            onWordStudySetToggled = { _, _ -> },
            onPronounceWord = {},
            onPresetStudySetToggled = {},
            onWordDeleted = {},
            onChangePresets = {},
            onEditWord = {},
            selection = WordSelection(),
            onDeleteSelected = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@LightDarkPreview
@Composable
private fun PresetDetailLoadingWordsPreview() {
    LexiconTheme {
        PresetDetailContent(
            uiState = PresetDetailUiState.Loaded(preset = previewPreset),
            onClose = {},
            onWordStudySetToggled = { _, _ -> },
            onPronounceWord = {},
            onPresetStudySetToggled = {},
            onWordDeleted = {},
            onChangePresets = {},
            onEditWord = {},
            selection = WordSelection(),
            onDeleteSelected = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@LightDarkPreview
@Composable
private fun PresetDetailNotFoundPreview() {
    LexiconTheme {
        PresetDetailContent(
            uiState = PresetDetailUiState.NotFound,
            onClose = {},
            onWordStudySetToggled = { _, _ -> },
            onPronounceWord = {},
            onPresetStudySetToggled = {},
            onWordDeleted = {},
            onChangePresets = {},
            onEditWord = {},
            selection = WordSelection(),
            onDeleteSelected = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
