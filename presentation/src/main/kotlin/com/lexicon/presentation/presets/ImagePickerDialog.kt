package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

object ImagePickerTestTags {
    const val DIALOG = "image_picker_dialog"
    const val GRID = "image_picker_grid"
}

private const val COLUMNS = 3

private val GridMinHeight = 240.dp
private val GridMaxHeight = 420.dp

/**
 * Choosing a picture, wherever it is chosen from. A grid rather than a row because a
 * single line shows three of what the web has for a word and hides the rest behind a
 * horizontal scroll nobody uses; and a dialog rather than a strip in the form because
 * picking a picture is its own decision, not a field to fill in passing.
 *
 * The next page is fetched when the last row comes into view, so the learner scrolls
 * instead of hunting for a More button.
 */
@Composable
fun ImagePickerDialog(
    candidates: ImmutableList<String>,
    ownImages: ImmutableList<String>,
    selected: String?,
    isLoading: Boolean,
    onSelected: (String) -> Unit,
    onOwnImageAdded: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grid = rememberLazyGridState()
    LoadWhenTheEndComesIntoView(grid = grid, isLoading = isLoading, onLoadMore = onLoadMore)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(ImagePickerTestTags.DIALOG),
        title = { Text(stringResource(R.string.create_word_image)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMNS),
                state = grid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GridMinHeight, max = GridMaxHeight)
                    .testTag(ImagePickerTestTags.GRID),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                item { AddImageTile(onPicked = onOwnImageAdded) }

                items(ownImages, key = { it }) { url ->
                    ImageCandidate(url = url, isSelected = url == selected, onClick = { onSelected(url) })
                }

                items(candidates, key = { it }) { url ->
                    ImageCandidate(url = url, isSelected = url == selected, onClick = { onSelected(url) })
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                }
            }
        },
    )

    if (candidates.isEmpty() && ownImages.isEmpty() && !isLoading) {
        Text(
            text = stringResource(R.string.create_word_image_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadWhenTheEndComesIntoView(
    grid: LazyGridState,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    val isNearTheEnd by remember(grid) {
        derivedStateOf {
            val lastVisible = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= grid.layoutInfo.totalItemsCount - COLUMNS
        }
    }

    LaunchedEffect(grid, isLoading) {
        snapshotFlow { isNearTheEnd }
            .distinctUntilChanged()
            .filter { it && !isLoading }
            .collect { onLoadMore() }
    }
}
