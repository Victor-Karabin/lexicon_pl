package com.lexicon.presentation.conjugation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.conjugation.VerbConjugation
import com.lexicon.presentation.R
import com.lexicon.presentation.common.DeleteAction
import com.lexicon.presentation.common.DeleteActionWidth
import com.lexicon.presentation.common.LoadMoreOnScroll
import com.lexicon.presentation.common.SwipeToRevealContainer
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.presets.WordRow
import com.lexicon.presentation.presets.detailLine
import com.lexicon.presentation.theme.Dimens
import org.koin.androidx.compose.koinViewModel

object VerbSelectionTestTags {
    const val SEARCH = "verb_selection_search"
    const val LIST = "verb_selection_list"
    const val RESTORE = "verb_selection_restore"

    fun verb(infinitive: String) = "verb_selection_item_$infinitive"
}

@Composable
fun VerbSelectionScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerbSelectionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VerbSelectionContent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onStatusCycled = viewModel::onStatusCycled,
        onPronounce = viewModel::onPronounce,
        onVerbDeleted = viewModel::onVerbDeleted,
        onMoreVerbs = viewModel::onMoreVerbs,
        onRestoreAll = viewModel::onRestoreAll,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun VerbSelectionContent(
    uiState: VerbSelectionUiState,
    onQueryChanged: (String) -> Unit,
    onStatusCycled: (VerbConjugation) -> Unit,
    onPronounce: (VerbConjugation) -> Unit,
    onVerbDeleted: (String) -> Unit,
    onMoreVerbs: () -> Unit,
    onRestoreAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrainingTopBar(
                title = stringResource(R.string.conjugation_select_title),
                onClose = onClose,
                actions = {
                    if (uiState.canRestore) {
                        TextButton(
                            onClick = onRestoreAll,
                            modifier = Modifier.testTag(VerbSelectionTestTags.RESTORE),
                        ) {
                            Text(stringResource(R.string.conjugation_restore_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.conjugation_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMedium)
                    .testTag(VerbSelectionTestTags.SEARCH),
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
                onLoadMore = onMoreVerbs,
            )

            LazyColumn(
                state = list,
                modifier = Modifier.weight(1f).testTag(VerbSelectionTestTags.LIST),
            ) {
                items(uiState.verbs, key = { it.infinitive }) { verb ->
                    SwipeToRevealContainer(
                        revealWidth = DeleteActionWidth,
                        backgroundContent = { DeleteAction(onClick = { onVerbDeleted(verb.infinitive) }) },
                    ) {
                        val word = uiState.words[verb.infinitive]
                        val partial = stringResource(R.string.conjugation_partial, verb.persons.size).takeUnless { verb.isComplete }
                        WordRow(
                            word = word,
                            text = verb.infinitive,
                            translation = verb.translation.orEmpty(),
                            status = uiState.statusOf(verb),
                            onStatusCycled = { onStatusCycled(verb) },
                            onPronounce = { onPronounce(verb) },
                            detail = listOfNotNull(word?.detailLine(), partial).joinToString(" · ").ifEmpty { null },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag(VerbSelectionTestTags.verb(verb.infinitive)),
                        )
                    }
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
