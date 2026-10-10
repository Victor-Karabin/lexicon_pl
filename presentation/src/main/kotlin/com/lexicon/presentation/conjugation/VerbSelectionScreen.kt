package com.lexicon.presentation.conjugation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.conjugation.VerbConjugation
import com.lexicon.presentation.R
import com.lexicon.presentation.common.DeleteAction
import com.lexicon.presentation.common.DeleteActionWidth
import com.lexicon.presentation.common.LoadMoreOnScroll
import com.lexicon.presentation.common.SwipeToRevealContainer
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import org.koin.androidx.compose.koinViewModel

object VerbSelectionTestTags {
    const val SEARCH = "verb_selection_search"
    const val LIST = "verb_selection_list"
    const val HINT = "verb_selection_hint"
    const val RESTORE = "verb_selection_restore"

    fun verb(infinitive: String) = "verb_selection_item_$infinitive"

    fun studySet(infinitive: String) = "verb_selection_study_set_$infinitive"
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
        onStudySetToggled = viewModel::onStudySetToggled,
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
    onStudySetToggled: (VerbConjugation) -> Unit,
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

            Text(
                text = stringResource(R.string.conjugation_study_set_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall)
                    .testTag(VerbSelectionTestTags.HINT),
            )

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
                        VerbRow(
                            verb = verb,
                            isInStudySet = verb.infinitive in uiState.studySet,
                            onStudySetToggled = { onStudySetToggled(verb) },
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

@Composable
private fun VerbRow(
    verb: VerbConjugation,
    isInStudySet: Boolean,
    onStudySetToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = isInStudySet, role = Role.Checkbox, onValueChange = { onStudySetToggled() })
            .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall)
            .testTag(VerbSelectionTestTags.verb(verb.infinitive)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = verb.infinitive,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            verb.translation?.let { translation ->
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!verb.isComplete) {
                Text(
                    text = stringResource(R.string.conjugation_partial, verb.persons.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Icon(
            imageVector = if (isInStudySet) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(
                if (isInStudySet) R.string.study_set_remove else R.string.study_set_add,
            ),
            tint = if (isInStudySet) LexiconError else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.minimumInteractiveComponentSize().testTag(VerbSelectionTestTags.studySet(verb.infinitive)),
        )
    }
}
