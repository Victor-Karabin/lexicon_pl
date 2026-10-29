package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LoadMoreOnScroll
import com.lexicon.presentation.theme.Dimens
import kotlinx.collections.immutable.ImmutableList

private const val COLUMNS = 2

private val GridMinHeight = 240.dp
private val GridMaxHeight = 420.dp

@Composable
fun ImagePickerDialog(
    candidates: ImmutableList<String>,
    ownImages: ImmutableList<String>,
    selected: String?,
    isLoading: Boolean,
    canLoadMore: Boolean,
    onSelected: (String) -> Unit,
    onOwnImageAdded: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grid = rememberLazyGridState()
    LoadMoreOnScroll(
        grid = grid,
        isLoading = isLoading,
        onLoadMore = onLoadMore,
        itemsBeforeTheEnd = COLUMNS,
        canLoadMore = canLoadMore,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.create_word_image)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMNS),
                state = grid,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GridMinHeight, max = GridMaxHeight),
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
