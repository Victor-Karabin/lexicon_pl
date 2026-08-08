package com.lexicon.presentation.presets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration.Companion.minutes

private val DetailIconSize = 56.dp

@Composable
fun PresetDetailScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    PresetDetailContent(
        uiState = uiState,
        onClose = onClose,
        onWordFavouriteToggled = viewModel::onWordFavouriteToggled,
        onPresetFavouriteToggled = viewModel::onPresetFavouriteToggled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDetailContent(
    uiState: PresetDetailUiState,
    onClose: () -> Unit,
    onWordFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    onPresetFavouriteToggled: (PresetFavouriteState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is PresetDetailUiState.Loaded -> uiState.preset.title.resolve(uiState.languageTag)
        else -> stringResource(R.string.preset_detail_title)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
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
                    PresetHeader(uiState, onPresetFavouriteToggled)
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
                            itemsIndexed(uiState.words, key = { _, word -> word.id.value }) { index, word ->
                                VocabularyWordRow(
                                    word = word,
                                    onFavouriteToggled = { onWordFavouriteToggled(word.id, !word.isFavourite) },
                                )
                                // Between rows only: a divider under the last one would read
                                // as the start of a section that is not there.
                                if (index < uiState.words.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun PresetHeader(
    uiState: PresetDetailUiState.Loaded,
    onFavouriteToggled: (PresetFavouriteState) -> Unit,
) {
    val preset = uiState.preset
    Row(
        modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(DetailIconSize).background(preset.detailAccentColor(), CircleShape),
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
                text = preset.description.resolve(uiState.languageTag),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.presets_card_meta,
                    preset.wordCount,
                    preset.cefr?.name ?: preset.category.title.resolve(uiState.languageTag),
                    preset.estimatedDuration.inWholeMinutes.toInt().coerceAtLeast(1),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
        }

        PresetFavouriteButton(
            state = uiState.favouriteState,
            onClick = { onFavouriteToggled(uiState.favouriteState) },
        )
    }
}

/** Falls back to the theme when a preset carries no colour, or one that will not parse. */
@Composable
private fun VocabularyPreset.detailAccentColor(): Color {
    val fallback = MaterialTheme.colorScheme.primary
    val hex = color?.removePrefix("#") ?: return fallback
    val parsed = hex.toLongOrNull(radix = 16) ?: return fallback
    return Color(parsed or 0xFF000000L)
}

private val previewPreset = VocabularyPreset(
    id = PresetId("food"),
    title = LocalizedText(mapOf("en" to "Food")),
    description = LocalizedText(mapOf("en" to "Meals, ingredients, fruit and vegetables.")),
    category = PresetCategory("everyday-life", 3, LocalizedText(mapOf("en" to "Everyday life"))),
    cefr = CefrLevel.A1,
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
                    PresetWord(VocabularyId(1), "chleb", "bread", "xlɛp", isFavourite = true),
                    PresetWord(VocabularyId(2), "jabłko", "apple", "ˈjabwkɔ"),
                    PresetWord(VocabularyId(3), "mleko", "milk", "ˈmlɛkɔ", isFavourite = true),
                    PresetWord(VocabularyId(4), "ziemniak", "potato", "ˈʑɛmɲak"),
                ),
                favouriteState = PresetFavouriteState.SOME,
            ),
            onClose = {},
            onWordFavouriteToggled = { _, _ -> },
            onPresetFavouriteToggled = {},
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
            onWordFavouriteToggled = { _, _ -> },
            onPresetFavouriteToggled = {},
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
            onWordFavouriteToggled = { _, _ -> },
            onPresetFavouriteToggled = {},
        )
    }
}
