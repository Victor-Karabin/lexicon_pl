package com.lexicon.presentation.presets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.interactors.presets.WordDraftProblem
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

private const val PRESET_CHIP_LINES = 2

private val CandidateSize = 104.dp
private val SelectedBorder = 3.dp
private val ImageRowHeight = 112.dp

@Composable
fun CreateWordScreen(
    onClose: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateWordViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedWord) {
        uiState.savedWord?.let(onCreated)
    }

    CreateWordContent(
        uiState = uiState,
        onClose = onClose,
        onTextChanged = viewModel::onTextChanged,
        onTranslationChanged = viewModel::onTranslationChanged,
        onImageSelected = viewModel::onImageSelected,
        onOwnImageAdded = viewModel::onOwnImageAdded,
        onMoreImages = viewModel::onMoreImages,
        onPresetToggled = viewModel::onPresetToggled,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

@Composable
private fun CreateWordContent(
    uiState: CreateWordUiState,
    onClose: () -> Unit,
    onTextChanged: (String) -> Unit,
    onTranslationChanged: (String) -> Unit,
    onImageSelected: (String) -> Unit,
    onOwnImageAdded: (String) -> Unit,
    onMoreImages: () -> Unit,
    onPresetToggled: (PresetId, Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrainingTopBar(
                title = stringResource(
                    if (uiState.isEditing) R.string.edit_word_title else R.string.create_word_title,
                ),
                onClose = onClose,
            )
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
                value = uiState.translation,
                onValueChange = onTranslationChanged,
                label = { Text(stringResource(R.string.create_word_base)) },
                singleLine = true,
                isError = uiState.problem == WordDraftProblem.MISSING_TRANSLATION,
                supportingText = {
                    when {
                        uiState.problem == WordDraftProblem.MISSING_TRANSLATION ->
                            Text(stringResource(R.string.create_word_translation_missing))

                        uiState.isTranslating -> Text(stringResource(R.string.create_translating))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = LexiconShapes.small,
            )

            OutlinedTextField(
                value = uiState.text,
                onValueChange = onTextChanged,
                label = { Text(stringResource(R.string.create_word_translation)) },
                singleLine = true,
                isError = uiState.problem != null && uiState.problem != WordDraftProblem.MISSING_TRANSLATION,
                supportingText = {
                    when (uiState.problem) {
                        WordDraftProblem.MISSING_TEXT -> Text(stringResource(R.string.create_word_text_missing))
                        WordDraftProblem.ALREADY_EXISTS ->
                            Text(stringResource(R.string.create_word_exists, uiState.text.trim()))

                        else -> Unit
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = LexiconShapes.small,
            )

            ImageSection(uiState, onImageSelected, onOwnImageAdded, onMoreImages)

            if (uiState.memberships.isNotEmpty()) {
                SectionHeading(stringResource(R.string.create_word_presets))
                PresetChips(
                    memberships = uiState.memberships,
                    languageTag = uiState.languageTag,
                    onToggle = onPresetToggled,
                    collapsedLines = PRESET_CHIP_LINES,
                )
            }
        }
    }
}

@Composable
private fun ImageSection(
    uiState: CreateWordUiState,
    onImageSelected: (String) -> Unit,
    onOwnImageAdded: (String) -> Unit,
    onMoreImages: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeading(stringResource(R.string.create_word_image))
        if (uiState.imageCandidates.isNotEmpty()) {
            TextButton(onClick = onMoreImages, enabled = !uiState.isLoadingImages) {
                Text(stringResource(R.string.create_word_image_more))
            }
        }
    }

    val scroll = rememberScrollState()
    RevealNewCandidates(
        candidates = uiState.imageCandidates,
        scroll = scroll,
        leadingTiles = 1 + uiState.ownImages.size,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        AddImageTile(onPicked = onOwnImageAdded)

        uiState.ownImages.forEach { url ->
            ImageCandidate(
                url = url,
                isSelected = url == uiState.selectedImage,
                onClick = { onImageSelected(url) },
            )
        }
        uiState.imageCandidates.forEach { url ->
            ImageCandidate(
                url = url,
                isSelected = url == uiState.selectedImage,
                onClick = { onImageSelected(url) },
            )
        }
        if (uiState.isLoadingImages) {
            Box(
                modifier = Modifier.size(CandidateSize),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }

    if (uiState.imageCandidates.isEmpty() && !uiState.isLoadingImages) {
        Text(
            text = stringResource(
                if (uiState.hasSearchedImages) {
                    R.string.create_word_image_empty
                } else {
                    R.string.create_word_image_none
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RevealNewCandidates(
    candidates: ImmutableList<String>,
    scroll: ScrollState,
    leadingTiles: Int,
) {
    val tileWidth = with(LocalDensity.current) { (CandidateSize + Dimens.spacingSmall).toPx() }
    var shown by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(candidates) {
        val appended = candidates.size > shown.size && candidates.take(shown.size) == shown
        when {
            appended -> scroll.animateScrollTo(((leadingTiles + shown.size) * tileWidth).roundToInt())
            candidates != shown -> scroll.scrollTo(0)
        }
        shown = candidates
    }
}

@Composable
private fun AddImageTile(onPicked: (String) -> Unit) {
    var isChoosing by remember { mutableStateOf(false) }
    val picker = rememberOwnImagePicker(onPicked = onPicked)

    Box {
        Surface(
            shape = LexiconShapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .size(CandidateSize)
                .clickable { isChoosing = true },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_word_image_add),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(expanded = isChoosing, onDismissRequest = { isChoosing = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.create_word_image_from_library)) },
                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                onClick = {
                    isChoosing = false
                    picker.pickFromLibrary()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.create_word_image_from_camera)) },
                leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                onClick = {
                    isChoosing = false
                    picker.takePhoto()
                },
            )
        }
    }
}

@Composable
private fun ImageCandidate(
    url: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = LexiconShapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(CandidateSize)
            .then(
                if (isSelected) {
                    Modifier.border(SelectedBorder, MaterialTheme.colorScheme.primary, LexiconShapes.small)
                } else {
                    Modifier
                },
            ).clickable(onClick = onClick),
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = if (isSelected) stringResource(R.string.create_word_image_selected) else null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            error = {},
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun previewMembership(
    id: String,
    title: String,
    isMember: Boolean,
) = PresetMembership(
    preset = VocabularyPreset(
        id = PresetId(id),
        title = LocalizedText(mapOf("en" to title)),
        description = LocalizedText(emptyMap()),
        category = PresetCategory("everyday-life", 3, LocalizedText(mapOf("en" to "Everyday life"))),
        icon = "category",
        color = "#2E7D32",
        popularity = 1,
        estimatedDuration = 10.minutes,
        vocabularyIds = persistentListOf(VocabularyId(1)),
    ),
    isMember = isMember,
)

@LightDarkPreview
@Composable
private fun CreateWordPreview() {
    LexiconTheme {
        CreateWordContent(
            uiState = CreateWordUiState(
                text = "woda",
                translation = "water",
                memberships = listOf(
                    previewMembership("food", "Food", isMember = true),
                    previewMembership("nature", "Nature", isMember = false),
                    previewMembership("top-100", "Top 100", isMember = false),
                ).toImmutableList(),
                hasSearchedImages = true,
            ),
            onClose = {},
            onTextChanged = {},
            onTranslationChanged = {},
            onImageSelected = {},
            onOwnImageAdded = {},
            onMoreImages = {},
            onPresetToggled = { _, _ -> },
            onSave = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CreateWordEmptyPreview() {
    LexiconTheme {
        CreateWordContent(
            uiState = CreateWordUiState(),
            onClose = {},
            onTextChanged = {},
            onTranslationChanged = {},
            onImageSelected = {},
            onOwnImageAdded = {},
            onMoreImages = {},
            onPresetToggled = { _, _ -> },
            onSave = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CreateWordDuplicatePreview() {
    LexiconTheme {
        CreateWordContent(
            uiState = CreateWordUiState(
                text = "woda",
                translation = "water",
                problem = WordDraftProblem.ALREADY_EXISTS,
                hasSearchedImages = true,
            ),
            onClose = {},
            onTextChanged = {},
            onTranslationChanged = {},
            onImageSelected = {},
            onOwnImageAdded = {},
            onMoreImages = {},
            onPresetToggled = { _, _ -> },
            onSave = {},
        )
    }
}
