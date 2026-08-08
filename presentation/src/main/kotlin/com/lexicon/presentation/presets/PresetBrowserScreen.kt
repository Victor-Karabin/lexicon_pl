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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetSort
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val IconBadgeSize = 44.dp

@Composable
fun PresetBrowserScreen(
    onPresetSelected: (PresetId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetBrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    PresetBrowserContent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onCategoryToggled = viewModel::onCategoryToggled,
        onCefrToggled = viewModel::onCefrToggled,
        onSortSelected = viewModel::onSortSelected,
        onFiltersCleared = viewModel::onFiltersCleared,
        onPresetSelected = onPresetSelected,
        onPresetFavouriteToggled = viewModel::onPresetFavouriteToggled,
        modifier = modifier,
    )
}

@Composable
private fun PresetBrowserContent(
    uiState: PresetBrowserUiState,
    onQueryChanged: (String) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onCefrToggled: (CefrLevel) -> Unit,
    onSortSelected: (PresetSort) -> Unit,
    onFiltersCleared: () -> Unit,
    onPresetSelected: (PresetId) -> Unit,
    onPresetFavouriteToggled: (PresetId, PresetFavouriteState) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is PresetBrowserUiState.Loading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        is PresetBrowserUiState.Loaded ->
            Column(modifier = modifier.fillMaxSize()) {
                SearchRow(
                    query = uiState.query,
                    sort = uiState.sort,
                    onQueryChanged = onQueryChanged,
                    onSortSelected = onSortSelected,
                )
                FilterRow(
                    uiState = uiState,
                    onCategoryToggled = onCategoryToggled,
                    onCefrToggled = onCefrToggled,
                    onFiltersCleared = onFiltersCleared,
                )

                when {
                    uiState.isEmptyCatalog -> Message(stringResource(R.string.presets_catalog_empty))
                    uiState.isEmptyResult -> Message(stringResource(R.string.presets_no_matches))
                    else ->
                        LazyColumn(
                            contentPadding = PaddingValues(Dimens.spacingMedium),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                        ) {
                            items(uiState.presets, key = { it.id.value }) { preset ->
                                val favouriteState = favouriteStateOf(preset, uiState.favouriteWordIds)
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
}

@Composable
private fun SearchRow(
    query: String,
    sort: PresetSort,
    onQueryChanged: (String) -> Unit,
    onSortSelected: (PresetSort) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            start = Dimens.spacingMedium,
            end = Dimens.spacingSmall,
            top = Dimens.spacingMedium,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VocabularySearchField(
            query = query,
            placeholder = stringResource(R.string.presets_search_hint),
            onQueryChanged = onQueryChanged,
            modifier = Modifier.weight(1f),
        )
        SortMenu(sort = sort, onSortSelected = onSortSelected)
    }
}

@Composable
private fun SortMenu(
    sort: PresetSort,
    onSortSelected: (PresetSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.presets_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PresetSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes()),
                            fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Categories and levels share one scrolling row: there are eleven categories and six
 * levels, and stacking them in a wrapping grid would push the presets themselves below
 * the fold on a phone.
 */
@Composable
private fun FilterRow(
    uiState: PresetBrowserUiState.Loaded,
    onCategoryToggled: (String) -> Unit,
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
        if (uiState.hasActiveFilters) {
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
        uiState.categories.forEach { category ->
            FilterChip(
                selected = category.id in uiState.selectedCategoryIds,
                onClick = { onCategoryToggled(category.id) },
                label = { Text(category.title.resolve(uiState.languageTag)) },
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
                modifier = Modifier
                    .size(IconBadgeSize)
                    .background(preset.accentColor(), CircleShape),
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
                        preset.cefr?.name ?: preset.category.title.resolve(languageTag),
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

@Composable
private fun Message(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
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

private fun PresetSort.labelRes(): Int =
    when (this) {
        PresetSort.POPULARITY -> R.string.presets_sort_popularity
        PresetSort.ALPHABETICAL -> R.string.presets_sort_alphabetical
        PresetSort.WORD_COUNT_ASCENDING -> R.string.presets_sort_fewest_words
        PresetSort.WORD_COUNT_DESCENDING -> R.string.presets_sort_most_words
    }

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
    cefr: CefrLevel? = null,
) = VocabularyPreset(
    id = PresetId(id),
    title = LocalizedText(mapOf("en" to title)),
    description = LocalizedText(mapOf("en" to description)),
    category = previewCategory,
    cefr = cefr,
    icon = icon,
    color = color,
    popularity = 1,
    estimatedDuration = (words * 60).seconds,
    vocabularyIds = persistentListOf(),
).let { preset ->
    preset.copy(vocabularyIds = List(words) { com.lexicon.interactors.presets.VocabularyId(it.toLong()) }.toImmutableList())
}

@LightDarkPreview
@Composable
private fun PresetBrowserPreview() {
    LexiconTheme {
        PresetBrowserContent(
            uiState = PresetBrowserUiState.Loaded(
                presets = persistentListOf(
                    previewPreset(
                        "top-100",
                        "100 most common words",
                        "The hundred words you will meet first in almost any Polish sentence.",
                        100,
                        "trending_up",
                        "#2E7D32",
                    ),
                    previewPreset(
                        "cefr-a1",
                        "A1 — Beginner",
                        "First words: greetings, family, food, numbers and the present tense.",
                        420,
                        "school",
                        "#1565C0",
                        CefrLevel.A1,
                    ),
                    previewPreset(
                        "food",
                        "Food",
                        "Meals, ingredients, fruit and vegetables.",
                        58,
                        "restaurant",
                        "#EF6C00",
                    ),
                ),
                categories = persistentListOf(previewCategory),
            ),
            onQueryChanged = {},
            onCategoryToggled = {},
            onCefrToggled = {},
            onSortSelected = {},
            onFiltersCleared = {},
            onPresetSelected = {},
            onPresetFavouriteToggled = { _, _ -> },
        )
    }
}

@LightDarkPreview
@Composable
private fun PresetBrowserNoMatchesPreview() {
    LexiconTheme {
        PresetBrowserContent(
            uiState = PresetBrowserUiState.Loaded(
                categories = persistentListOf(previewCategory),
                query = "zzz",
            ),
            onQueryChanged = {},
            onCategoryToggled = {},
            onCefrToggled = {},
            onSortSelected = {},
            onFiltersCleared = {},
            onPresetSelected = {},
            onPresetFavouriteToggled = { _, _ -> },
        )
    }
}
