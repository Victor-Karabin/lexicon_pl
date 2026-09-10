package com.lexicon.presentation.presets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.WordDraftProblem
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.PresetCategory
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.VocabularyPreset
import com.lexicon.presentation.R
import com.lexicon.presentation.common.ExampleSentenceRow
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
private val ProgressSize = 18.dp
private val ProgressStroke = 2.dp
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
        onExampleChanged = viewModel::onExampleChanged,
        onExampleRequested = viewModel::onExampleRequested,
        onExamplePlayed = viewModel::onExamplePlayed,
        onPresetToggled = viewModel::onPresetToggled,
        onSave = viewModel::onSave,
        onErrorShown = viewModel::onErrorShown,
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
    onExampleChanged: (String) -> Unit,
    onExampleRequested: () -> Unit,
    onExamplePlayed: () -> Unit,
    onPresetToggled: (PresetId, Boolean) -> Unit,
    onSave: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }

    val exampleFailed = stringResource(R.string.example_offline)
    val presetFailed = stringResource(R.string.preset_change_failed)

    LaunchedEffect(uiState.exampleFailed, uiState.presetFailed) {
        val message = when {
            uiState.exampleFailed -> exampleFailed
            uiState.presetFailed -> presetFailed
            else -> return@LaunchedEffect
        }
        snackbar.showSnackbar(message)
        onErrorShown()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
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

            ExampleSection(
                uiState = uiState,
                onExampleChanged = onExampleChanged,
                onExampleRequested = onExampleRequested,
                onExamplePlayed = onExamplePlayed,
            )

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
private fun ExampleSection(
    uiState: CreateWordUiState,
    onExampleChanged: (String) -> Unit,
    onExampleRequested: () -> Unit,
    onExamplePlayed: () -> Unit,
) {
    SectionHeading(stringResource(R.string.example_label))

    if (!uiState.sentence.isBlank) {
        ExampleSentenceRow(
            example = uiState.sentence,
            onPlay = onExamplePlayed,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    OutlinedTextField(
        value = uiState.exampleInput,
        onValueChange = onExampleChanged,
        label = { Text(stringResource(R.string.example_hint)) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
        shape = LexiconShapes.small,
        trailingIcon = {
            if (uiState.isWritingExample) {
                CircularProgressIndicator(modifier = Modifier.size(ProgressSize), strokeWidth = ProgressStroke)
            } else {
                IconButton(onClick = onExampleRequested, enabled = uiState.canWriteExample) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(
                            if (uiState.example.isBlank()) R.string.example_generate else R.string.example_refresh,
                        ),
                    )
                }
            }
        },
    )
}

@Composable
private fun ImageSection(
    uiState: CreateWordUiState,
    onImageSelected: (String) -> Unit,
    onOwnImageAdded: (String) -> Unit,
    onMoreImages: () -> Unit,
) {
    var isPicking by remember { mutableStateOf(false) }

    SectionHeading(stringResource(R.string.create_word_image))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChosenImageTile(
            url = uiState.selectedImage,
            isLoading = uiState.isImageLoading,
            onClick = { isPicking = true },
        )

        TextButton(onClick = { isPicking = true }) {
            Text(
                stringResource(
                    if (uiState.selectedImage == null) {
                        R.string.create_word_image_choose
                    } else {
                        R.string.create_word_image_change
                    },
                ),
            )
        }
    }

    if (isPicking) {
        ImagePickerDialog(
            candidates = uiState.imageCandidates,
            ownImages = uiState.ownImages,
            selected = uiState.selectedImage,
            isLoading = uiState.isLoadingImages,
            canLoadMore = uiState.hasMoreImages,
            onSelected = onImageSelected,
            onOwnImageAdded = onOwnImageAdded,
            onLoadMore = onMoreImages,
            onDismiss = { isPicking = false },
        )
    }
}

@Composable
private fun ChosenImageTile(
    url: String?,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = LexiconShapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(CandidateSize).clickable(enabled = !isLoading, onClick = onClick),
    ) {
        if (url == null) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_word_image_add),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = stringResource(R.string.create_word_image_selected),
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
internal fun AddImageTile(onPicked: (String) -> Unit) {
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
internal fun ImageCandidate(
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
            onExampleChanged = {},
            onExampleRequested = {},
            onExamplePlayed = {},
            onPresetToggled = { _, _ -> },
            onSave = {},
            onErrorShown = {},
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
            onExampleChanged = {},
            onExampleRequested = {},
            onExamplePlayed = {},
            onPresetToggled = { _, _ -> },
            onSave = {},
            onErrorShown = {},
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
            onExampleChanged = {},
            onExampleRequested = {},
            onExamplePlayed = {},
            onPresetToggled = { _, _ -> },
            onSave = {},
            onErrorShown = {},
        )
    }
}
