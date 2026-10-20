package com.lexicon.presentation.declension

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordForms
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LoadMoreOnScroll
import com.lexicon.presentation.common.ScrollToTopClearance
import com.lexicon.presentation.common.ScrollToTopLayout
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.presets.WordRow
import com.lexicon.presentation.presets.detailLine
import com.lexicon.presentation.theme.Dimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeclensionWordsScreen(
    onOpenRules: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeclensionWordsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeclensionWordsContent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onStatusCycled = viewModel::onStatusCycled,
        onPronounce = viewModel::onPronounce,
        onMoreWords = viewModel::onMoreWords,
        onOpenRules = onOpenRules,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun DeclensionWordsContent(
    uiState: DeclensionWordsUiState,
    onQueryChanged: (String) -> Unit,
    onStatusCycled: (Word) -> Unit,
    onPronounce: (Word) -> Unit,
    onMoreWords: () -> Unit,
    onOpenRules: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrainingTopBar(
                title = stringResource(R.string.declension_select_title),
                onClose = onClose,
                actions = { CasesRuleButton(onClick = onOpenRules) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.declension_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMedium),
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSmall))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            val list = rememberLazyListState()
            LoadMoreOnScroll(
                list = list,
                isLoading = uiState.isLoadingMore,
                onLoadMore = onMoreWords,
            )

            ScrollToTopLayout(list = list, modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ScrollToTopClearance),
                ) {
                    items(uiState.words, key = { it.id.value }) { word ->
                        val kind = word.partOfSpeech?.let { wordKindLabel(it, (word.forms as? WordForms.Noun)?.gender) }
                        WordRow(
                            word = word,
                            text = word.text,
                            translation = word.translation,
                            status = uiState.statusOf(word),
                            onStatusCycled = { onStatusCycled(word) },
                            onPronounce = { onPronounce(word) },
                            detail = listOfNotNull(word.detailLine(), kind).joinToString(" · ").ifEmpty { null },
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
