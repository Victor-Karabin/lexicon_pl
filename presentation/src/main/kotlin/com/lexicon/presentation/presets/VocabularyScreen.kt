package com.lexicon.presentation.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SwipeToRevealContainer
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val IconBadgeSize = 44.dp
private val DeleteActionWidth = 88.dp
private val DeleteActionRadius = 12.dp

/** A typical word row: target, translation, transcription. */
private val DeleteActionPreviewHeight = 76.dp

/**
 * The Vocabulary tab: curated presets, and a search over every word.
 *
 * One list, not two. The search box looks up words — the thing you reach for when you want to
 * know or favourite a particular one — and the presets stay underneath it, restored untouched
 * the moment the box is cleared.
 */
@Composable
fun VocabularyScreen(
    onPresetSelected: (PresetId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VocabularyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val deleted = (uiState as? VocabularyUiState.Loaded)?.lastDeleted
    val deletedMessage = deleted?.let { stringResource(R.string.vocabulary_deleted, it.label) }
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

    VocabularyContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onQueryChanged = viewModel::onQueryChanged,
        onCefrToggled = viewModel::onCefrToggled,
        onFiltersCleared = viewModel::onFiltersCleared,
        onPresetSelected = onPresetSelected,
        onPresetFavouriteToggled = viewModel::onPresetFavouriteToggled,
        onWordFavouriteToggled = viewModel::onWordFavouriteToggled,
        onWordDeleted = viewModel::onWordDeleted,
        onPresetDeleted = viewModel::onPresetDeleted,
        modifier = modifier,
    )
}

@Composable
private fun VocabularyContent(
    uiState: VocabularyUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChanged: (String) -> Unit,
    onCefrToggled: (CefrLevel) -> Unit,
    onFiltersCleared: () -> Unit,
    onPresetSelected: (PresetId) -> Unit,
    onPresetFavouriteToggled: (PresetId, PresetFavouriteState) -> Unit,
    onWordFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    onWordDeleted: (PresetWord) -> Unit,
    onPresetDeleted: (VocabularyPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        VocabularyBody(
            uiState = uiState,
            onQueryChanged = onQueryChanged,
            onCefrToggled = onCefrToggled,
            onFiltersCleared = onFiltersCleared,
            onPresetSelected = onPresetSelected,
            onPresetFavouriteToggled = onPresetFavouriteToggled,
            onWordFavouriteToggled = onWordFavouriteToggled,
            onWordDeleted = onWordDeleted,
            onPresetDeleted = onPresetDeleted,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun VocabularyBody(
    uiState: VocabularyUiState,
    onQueryChanged: (String) -> Unit,
    onCefrToggled: (CefrLevel) -> Unit,
    onFiltersCleared: () -> Unit,
    onPresetSelected: (PresetId) -> Unit,
    onPresetFavouriteToggled: (PresetId, PresetFavouriteState) -> Unit,
    onWordFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    onWordDeleted: (PresetWord) -> Unit,
    onPresetDeleted: (VocabularyPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is VocabularyUiState.Loading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        is VocabularyUiState.Loaded ->
            Column(modifier = modifier.fillMaxSize()) {
                VocabularySearchField(
                    query = uiState.query,
                    placeholder = stringResource(R.string.vocabulary_search_hint),
                    onQueryChanged = onQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingMedium),
                )

                FilterRow(uiState, onCefrToggled, onFiltersCleared)

                if (uiState.isSearchingWords) {
                    WordResults(uiState, onWordFavouriteToggled, onWordDeleted)
                } else {
                    PresetResults(uiState, onPresetSelected, onPresetFavouriteToggled, onPresetDeleted)
                }
            }
    }
}

@Composable
private fun WordResults(
    uiState: VocabularyUiState.Loaded,
    onWordFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    onWordDeleted: (PresetWord) -> Unit,
) {
    if (uiState.hasNoMatchingWords) {
        Message(stringResource(R.string.vocabulary_search_no_matches, uiState.query))
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = Dimens.spacingSmall)) {
        itemsIndexed(uiState.words, key = { _, word -> word.id.value }) { index, word ->
            SwipeToRevealContainer(
                revealWidth = DeleteActionWidth,
                backgroundContent = { DeleteAction(onClick = { onWordDeleted(word) }) },
            ) {
                VocabularyWordRow(
                    word = word,
                    onFavouriteToggled = { onWordFavouriteToggled(word.id, !word.isFavourite) },
                )
            }
            if (index < uiState.words.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
            }
        }
    }
}

@Composable
private fun PresetResults(
    uiState: VocabularyUiState.Loaded,
    onPresetSelected: (PresetId) -> Unit,
    onPresetFavouriteToggled: (PresetId, PresetFavouriteState) -> Unit,
    onPresetDeleted: (VocabularyPreset) -> Unit,
) {
    when {
        uiState.hasNoPresetsAtAll -> Message(stringResource(R.string.presets_catalog_empty))
        else ->
            LazyColumn(
                contentPadding = PaddingValues(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                items(uiState.presets, key = { it.id.value }) { preset ->
                    val favouriteState = favouriteStateOf(preset, uiState.favouriteWordIds)
                    SwipeToRevealContainer(
                        revealWidth = DeleteActionWidth,
                        backgroundContent = { DeleteAction(onClick = { onPresetDeleted(preset) }) },
                    ) {
                        PresetCard(
                            preset = preset,
                            languageTag = uiState.languageTag,
                            favouriteState = favouriteState,
                            onClick = { onPresetSelected(preset.id) },
                            onFavouriteToggled = { onPresetFavouriteToggled(preset.id, favouriteState) },
                        )
                    }
                }
            }
    }
}

/**
 * The CEFR levels, in one scrolling row. They select words rather than narrowing presets, so
 * they stay reachable whichever list is on screen.
 */
@Composable
private fun FilterRow(
    uiState: VocabularyUiState.Loaded,
    onCefrToggled: (CefrLevel) -> Unit,
    onFiltersCleared: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (uiState.selectedCefrLevels.isNotEmpty()) {
            FilterChip(
                selected = false,
                onClick = onFiltersCleared,
                label = { Text(stringResource(R.string.presets_clear_filters)) },
                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
            )
        }
        CefrLevel.entries.forEach { level ->
            FilterChip(
                selected = level in uiState.selectedCefrLevels,
                onClick = { onCefrToggled(level) },
                label = { Text(level.name) },
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: VocabularyPreset,
    languageTag: String,
    favouriteState: PresetFavouriteState,
    onClick: () -> Unit,
    onFavouriteToggled: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = LexiconShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            Box(
                modifier = Modifier.size(IconBadgeSize).background(preset.accentColor(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = presetIconFor(preset.icon),
                    contentDescription = null,
                    tint = Color.White,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title.resolve(languageTag),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = preset.description.resolve(languageTag),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Dimens.spacingTiny),
                )
                Text(
                    text = stringResource(
                        R.string.presets_card_meta,
                        preset.wordCount,
                        preset.category.title.resolve(languageTag),
                        preset.estimatedDuration.readableMinutes(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                )
            }

            PresetFavouriteButton(state = favouriteState, onClick = onFavouriteToggled)
        }
    }
}

/**
 * The action the swipe uncovers. A button rather than the swipe itself doing the deleting, so a
 * gesture made by accident costs nothing.
 */
@Composable
private fun DeleteAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = LexiconError,
        // Rounded on the revealed edge only: the other side is butted against the row that
        // slid away, and rounding it would leave a notch in the middle of the list.
        shape = RoundedCornerShape(topEnd = DeleteActionRadius, bottomEnd = DeleteActionRadius),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
            Text(
                text = stringResource(R.string.vocabulary_delete),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Falls back to the theme when a preset carries no colour, or one that will not parse. */
@Composable
private fun VocabularyPreset.accentColor(): Color {
    val fallback = MaterialTheme.colorScheme.primary
    val hex = color?.removePrefix("#") ?: return fallback
    val parsed = hex.toLongOrNull(radix = 16) ?: return fallback
    return Color(parsed or 0xFF000000L)
}

private fun Duration.readableMinutes(): Int = inWholeMinutes.toInt().coerceAtLeast(1)

private val previewCategory = PresetCategory(
    id = "everyday-life",
    order = 3,
    title = LocalizedText(mapOf("en" to "Everyday life")),
)

private fun previewPreset(
    id: String,
    title: String,
    description: String,
    words: Int,
    icon: String,
    color: String,
) = VocabularyPreset(
    id = PresetId(id),
    title = LocalizedText(mapOf("en" to title)),
    description = LocalizedText(mapOf("en" to description)),
    category = previewCategory,
    icon = icon,
    color = color,
    popularity = 1,
    estimatedDuration = words.minutes,
    vocabularyIds = List(words) { VocabularyId(it.toLong()) }.toImmutableList(),
)

private val previewPresets = persistentListOf(
    previewPreset(
        "top-100",
        "100 most common words",
        "The hundred words you will meet first in almost any Polish sentence.",
        100,
        "trending_up",
        "#2E7D32",
    ),
    previewPreset(
        "greetings",
        "Greetings",
        "Hello, goodbye, thank you — the phrases every conversation starts with.",
        20,
        "waving_hand",
        "#EF6C00",
    ),
    previewPreset(
        "food",
        "Food",
        "Meals, ingredients, fruit and vegetables.",
        58,
        "restaurant",
        "#EF6C00",
    ),
)

/**
 * The action at the size the gesture uncovers. Worth its own preview because it is only ever
 * on screen mid-swipe, which no screen preview can reach.
 */
@LightDarkPreview
@Composable
private fun DeleteActionPreview() {
    LexiconTheme {
        Box(modifier = Modifier.width(DeleteActionWidth).height(DeleteActionPreviewHeight)) {
            DeleteAction(onClick = {})
        }
    }
}

@LightDarkPreview
@Composable
private fun VocabularyPresetsPreview() {
    LexiconTheme {
        VocabularyContent(
            uiState = VocabularyUiState.Loaded(
                presets = previewPresets,
            ),
            onQueryChanged = {},
            onCefrToggled = {},
            onFiltersCleared = {},
            onPresetSelected = {},
            onPresetFavouriteToggled = { _, _ -> },
            onWordFavouriteToggled = { _, _ -> },
            onWordDeleted = {},
            onPresetDeleted = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@LightDarkPreview
@Composable
private fun VocabularyWordSearchPreview() {
    LexiconTheme {
        VocabularyContent(
            uiState = VocabularyUiState.Loaded(
                query = "wod",
                presets = previewPresets,
                words = persistentListOf(
                    PresetWord(VocabularyId(1), "woda", "water", "ˈvɔda", isFavourite = true, cefr = CefrLevel.A1),
                    PresetWord(VocabularyId(2), "wodospad", "waterfall", "vɔˈdɔspat", cefr = CefrLevel.B1),
                    PresetWord(VocabularyId(3), "woda mineralna", "mineral water", "ˈvɔda miɲɛˈralna", cefr = CefrLevel.A1),
                ),
            ),
            onQueryChanged = {},
            onCefrToggled = {},
            onFiltersCleared = {},
            onPresetSelected = {},
            onPresetFavouriteToggled = { _, _ -> },
            onWordFavouriteToggled = { _, _ -> },
            onWordDeleted = {},
            onPresetDeleted = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@LightDarkPreview
@Composable
private fun VocabularyNoMatchingWordsPreview() {
    LexiconTheme {
        VocabularyContent(
            uiState = VocabularyUiState.Loaded(query = "qqq", presets = previewPresets),
            onQueryChanged = {},
            onCefrToggled = {},
            onFiltersCleared = {},
            onPresetSelected = {},
            onPresetFavouriteToggled = { _, _ -> },
            onWordFavouriteToggled = { _, _ -> },
            onWordDeleted = {},
            onPresetDeleted = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
