package com.lexicon.presentation.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetDraftProblem
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetStudySetState
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.presentation.R
import com.lexicon.presentation.common.ExpandableFlowRow
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.seconds

private const val ICON_LINES = 3
private const val COLOR_LINES = 3

private val SwatchSize = 44.dp
private val IconChoiceSize = 48.dp
private val SelectedBorderWidth = 3.dp

@Composable
fun CreatePresetScreen(
    onClose: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePresetViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedName) {
        uiState.savedName?.let(onCreated)
    }

    CreatePresetContent(
        uiState = uiState,
        onClose = onClose,
        onNameChanged = viewModel::onNameChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onIconSelected = viewModel::onIconSelected,
        onColorSelected = viewModel::onColorSelected,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

@Composable
private fun CreatePresetContent(
    uiState: CreatePresetUiState,
    onClose: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TrainingTopBar(title = stringResource(R.string.create_preset_title), onClose = onClose)

                val preview = uiState.asPreview()
                val skin = presetTileSkin(preview)
                GradientTile(
                    skin = skin,
                    modifier = Modifier.padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
                ) {
                    PresetSummary(
                        preset = preview,
                        languageTag = LocalizedText.DEFAULT_LANGUAGE,
                        studySetState = PresetStudySetState.NONE,
                        skin = skin,
                        onStudySetToggled = {},
                    )
                }
            }
        },
        bottomBar = {
            TextButton(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingMedium),
            ) {
                Text(stringResource(R.string.create_save))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = { Text(stringResource(R.string.create_preset_name)) },
                singleLine = true,
                isError = uiState.problem == PresetDraftProblem.MISSING_TITLE,
                supportingText = {
                    if (uiState.problem == PresetDraftProblem.MISSING_TITLE) {
                        Text(stringResource(R.string.create_preset_name_missing))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = LexiconShapes.small,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                label = { Text(stringResource(R.string.create_preset_description)) },
                modifier = Modifier.fillMaxWidth(),
                shape = LexiconShapes.small,
            )

            SectionLabel(stringResource(R.string.create_preset_icon))
            IconChoices(selected = uiState.icon, accent = uiState.color, onSelected = onIconSelected)

            SectionLabel(stringResource(R.string.create_preset_color))
            ColorChoices(selected = uiState.color, onSelected = onColorSelected)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun IconChoices(
    selected: String,
    accent: String,
    onSelected: (String) -> Unit,
) {
    val accentColor = accent.toAccentColor(MaterialTheme.colorScheme.primary)
    ExpandableFlowRow(collapsedLines = ICON_LINES) {
        PRESET_ICON_CHOICES.forEach { icon ->
            val isSelected = icon == selected
            Box(
                modifier = Modifier
                    .size(IconChoiceSize)
                    .background(
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                    ).selectable(selected = isSelected, onClick = { onSelected(icon) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = presetIconFor(icon),
                    contentDescription = icon,
                    tint = if (isSelected) onAccentColor(accentColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ColorChoices(
    selected: String,
    onSelected: (String) -> Unit,
) {
    ExpandableFlowRow(collapsedLines = COLOR_LINES) {
        PRESET_COLOR_CHOICES.forEach { hex ->
            val isSelected = hex == selected
            Box(
                modifier = Modifier
                    .size(SwatchSize)
                    .background(hex.toAccentColor(MaterialTheme.colorScheme.primary), CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(SelectedBorderWidth, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        },
                    ).selectable(selected = isSelected, onClick = { onSelected(hex) }),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = onAccentColor(hex.toAccentColor(MaterialTheme.colorScheme.primary)),
                    )
                }
            }
        }
    }
}

private fun CreatePresetUiState.asPreview(): VocabularyPreset =
    VocabularyPreset(
        id = PresetId(""),
        title = LocalizedText(mapOf(LocalizedText.DEFAULT_LANGUAGE to name.ifBlank { " " })),
        description = LocalizedText(mapOf(LocalizedText.DEFAULT_LANGUAGE to description)),
        category = PresetCategory("", 0, LocalizedText(emptyMap())),
        icon = icon,
        color = color,
        popularity = 0,
        estimatedDuration = 0.seconds,
        vocabularyIds = persistentListOf(),
    )

@LightDarkPreview
@Composable
private fun CreatePresetPreview() {
    LexiconTheme {
        CreatePresetContent(
            uiState = CreatePresetUiState(name = "Kitchen", description = "Pots, pans and what goes in them."),
            onClose = {},
            onNameChanged = {},
            onDescriptionChanged = {},
            onIconSelected = {},
            onColorSelected = {},
            onSave = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CreatePresetEmptyPreview() {
    LexiconTheme {
        CreatePresetContent(
            uiState = CreatePresetUiState(problem = PresetDraftProblem.MISSING_TITLE),
            onClose = {},
            onNameChanged = {},
            onDescriptionChanged = {},
            onIconSelected = {},
            onColorSelected = {},
            onSave = {},
        )
    }
}
