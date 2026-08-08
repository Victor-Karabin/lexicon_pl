package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf

private enum class VocabularySection { PRESETS, WORDS }

private val SectionSaver = Saver<VocabularySection, String>(
    save = { it.name },
    restore = { VocabularySection.valueOf(it) },
)

/**
 * The Vocabulary tab: curated collections, or the whole vocabulary searched word by word.
 *
 * Two sections rather than one merged list, because the two searches answer different
 * questions — "what should I study" and "what does this word mean" — and a result list mixing
 * presets with words would make neither easy to scan.
 */
@Composable
fun VocabularyScreen(
    onPresetSelected: (PresetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by rememberSaveable(stateSaver = SectionSaver) {
        mutableStateOf(VocabularySection.PRESETS)
    }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
        ) {
            VocabularySection.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = section == entry,
                    onClick = { section = entry },
                    shape = SegmentedButtonDefaults.itemShape(index, VocabularySection.entries.size),
                ) {
                    Text(
                        stringResource(
                            when (entry) {
                                VocabularySection.PRESETS -> R.string.vocabulary_section_presets
                                VocabularySection.WORDS -> R.string.vocabulary_section_words
                            },
                        ),
                    )
                }
            }
        }

        when (section) {
            VocabularySection.PRESETS -> PresetBrowserScreen(onPresetSelected = onPresetSelected)
            VocabularySection.WORDS -> VocabularySearchScreen()
        }
    }
}

@Composable
private fun VocabularySearchScreen(viewModel: VocabularySearchViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    VocabularySearchContent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onFavouriteToggled = viewModel::onFavouriteToggled,
    )
}

@Composable
private fun VocabularySearchContent(
    uiState: VocabularySearchUiState,
    onQueryChanged: (String) -> Unit,
    onFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        VocabularySearchField(
            query = uiState.query,
            placeholder = stringResource(R.string.vocabulary_search_hint),
            onQueryChanged = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
        )

        when {
            // Before anything is typed the list would be the whole vocabulary, which buries
            // the field that is about to be used; the prompt explains what to do instead.
            !uiState.hasQuery -> Message(stringResource(R.string.vocabulary_search_prompt))
            uiState.isEmptyResult -> Message(stringResource(R.string.vocabulary_search_no_matches, uiState.query))
            else ->
                LazyColumn(contentPadding = PaddingValues(vertical = Dimens.spacingSmall)) {
                    itemsIndexed(uiState.results, key = { _, word -> word.id.value }) { index, word ->
                        VocabularyWordRow(
                            word = word,
                            onFavouriteToggled = { onFavouriteToggled(word.id, !word.isFavourite) },
                        )
                        if (index < uiState.results.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
                        }
                    }
                }
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

@LightDarkPreview
@Composable
private fun VocabularySearchResultsPreview() {
    LexiconTheme {
        VocabularySearchContent(
            uiState = VocabularySearchUiState(
                query = "wod",
                results = persistentListOf(
                    PresetWord(VocabularyId(1), "woda", "water", "ˈvɔda", isFavourite = true),
                    PresetWord(VocabularyId(2), "wodospad", "waterfall", "vɔˈdɔspat"),
                    PresetWord(VocabularyId(3), "woda mineralna", "mineral water", "ˈvɔda miɲɛˈralna"),
                ),
            ),
            onQueryChanged = {},
            onFavouriteToggled = { _, _ -> },
        )
    }
}

@LightDarkPreview
@Composable
private fun VocabularySearchEmptyPreview() {
    LexiconTheme {
        VocabularySearchContent(
            uiState = VocabularySearchUiState(),
            onQueryChanged = {},
            onFavouriteToggled = { _, _ -> },
        )
    }
}

@LightDarkPreview
@Composable
private fun VocabularySearchNoMatchesPreview() {
    LexiconTheme {
        VocabularySearchContent(
            uiState = VocabularySearchUiState(query = "qqq"),
            onQueryChanged = {},
            onFavouriteToggled = { _, _ -> },
        )
    }
}
