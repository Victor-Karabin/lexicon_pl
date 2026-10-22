package com.lexicon.presentation.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val ROWS_BEFORE_THE_END = 5

@Composable
fun LoadMoreOnScroll(
    list: LazyListState,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    rowsBeforeTheEnd: Int = ROWS_BEFORE_THE_END,
) {
    LoadMoreNearTheEnd(
        state = list,
        isLoading = isLoading,
        canLoadMore = true,
        itemsBeforeTheEnd = rowsBeforeTheEnd,
        lastVisibleIndex = { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index },
        totalItemsCount = { list.layoutInfo.totalItemsCount },
        onLoadMore = onLoadMore,
    )
}

@Composable
fun LoadMoreOnScroll(
    grid: LazyGridState,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    itemsBeforeTheEnd: Int,
    canLoadMore: Boolean = true,
) {
    LoadMoreNearTheEnd(
        state = grid,
        isLoading = isLoading,
        canLoadMore = canLoadMore,
        itemsBeforeTheEnd = itemsBeforeTheEnd,
        lastVisibleIndex = { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index },
        totalItemsCount = { grid.layoutInfo.totalItemsCount },
        onLoadMore = onLoadMore,
    )
}

@Composable
private fun LoadMoreNearTheEnd(
    state: Any,
    isLoading: Boolean,
    canLoadMore: Boolean,
    itemsBeforeTheEnd: Int,
    lastVisibleIndex: () -> Int?,
    totalItemsCount: () -> Int,
    onLoadMore: () -> Unit,
) {
    val isNearTheEnd by remember(state, itemsBeforeTheEnd) {
        derivedStateOf {
            val lastVisible = lastVisibleIndex() ?: return@derivedStateOf false
            lastVisible >= totalItemsCount() - itemsBeforeTheEnd
        }
    }

    LaunchedEffect(state, isLoading, canLoadMore) {
        snapshotFlow { isNearTheEnd }
            .distinctUntilChanged()
            .filter { it && !isLoading && canLoadMore }
            .collect { onLoadMore() }
    }
}
