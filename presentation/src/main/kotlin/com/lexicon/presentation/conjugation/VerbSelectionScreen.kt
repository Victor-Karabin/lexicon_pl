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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.lexicon.interactors.conjugation.VerbConjugation
import com.lexicon.presentation.R
import com.lexicon.presentation.common.DeleteAction
import com.lexicon.presentation.common.DeleteActionWidth
import com.lexicon.presentation.common.SwipeToRevealContainer
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import org.koin.androidx.compose.koinViewModel

object VerbSelectionTestTags {
    const val SEARCH = "verb_selection_search"
    const val LIST = "verb_selection_list"
    const val COUNT = "verb_selection_count"
    const val CONTINUE = "verb_selection_continue"
    const val RESTORE = "verb_selection_restore"

    fun verb(infinitive: String) = "verb_selection_item_$infinitive"

    fun checkbox(infinitive: String) = "verb_selection_checkbox_$infinitive"

    fun favourite(infinitive: String) = "verb_selection_favourite_$infinitive"
}

@Composable
fun VerbSelectionScreen(
    onContinue: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerbSelectionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSaveHandled()
            onContinue()
        }
    }

    VerbSelectionContent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onVerbToggled = viewModel::onVerbToggled,
        onFavouriteToggled = viewModel::onFavouriteToggled,
        onCreateCourse = viewModel::onCreateCourse,
        onVerbDeleted = viewModel::onVerbDeleted,
        onRestoreAll = viewModel::onRestoreAll,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun VerbSelectionContent(
    uiState: VerbSelectionUiState,
    onQueryChanged: (String) -> Unit,
    onVerbToggled: (String) -> Unit,
    onFavouriteToggled: (VerbConjugation) -> Unit,
    onCreateCourse: () -> Unit,
    onVerbDeleted: (String) -> Unit,
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
                text = stringResource(R.string.conjugation_selected_count, uiState.count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall)
                    .testTag(VerbSelectionTestTags.COUNT),
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            LazyColumn(modifier = Modifier.weight(1f).testTag(VerbSelectionTestTags.LIST)) {
                items(uiState.verbs, key = { it.infinitive }) { verb ->
                    SwipeToRevealContainer(
                        revealWidth = DeleteActionWidth,
                        backgroundContent = { DeleteAction(onClick = { onVerbDeleted(verb.infinitive) }) },
                    ) {
                        VerbRow(
                            verb = verb,
                            isSelected = verb.infinitive in uiState.selected,
                            isFavourite = verb.infinitive in uiState.favourites,
                            onToggled = { onVerbToggled(verb.infinitive) },
                            onFavouriteToggled = { onFavouriteToggled(verb) },
                        )
                    }
                }
            }

            Button(
                onClick = onCreateCourse,
                enabled = uiState.canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingMedium)
                    .testTag(VerbSelectionTestTags.CONTINUE),
            ) {
                Text(stringResource(R.string.conjugation_create))
            }
        }
    }
}

@Composable
private fun VerbRow(
    verb: VerbConjugation,
    isSelected: Boolean,
    isFavourite: Boolean,
    onToggled: () -> Unit,
    onFavouriteToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggled() })
            .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall)
            .testTag(VerbSelectionTestTags.verb(verb.infinitive)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            modifier = Modifier.testTag(VerbSelectionTestTags.checkbox(verb.infinitive)),
        )
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

        IconButton(
            onClick = onFavouriteToggled,
            modifier = Modifier.testTag(VerbSelectionTestTags.favourite(verb.infinitive)),
        ) {
            Icon(
                imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavourite) R.string.favourite_remove else R.string.favourite_add,
                ),
                tint = if (isFavourite) LexiconError else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
