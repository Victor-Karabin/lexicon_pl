package com.lexicon.presentation.common

import androidx.compose.foundation.lazy.LazyListState
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
    val isNearTheEnd by remember(list, rowsBeforeTheEnd) {
        derivedStateOf {
            val lastVisible = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= list.layoutInfo.totalItemsCount - rowsBeforeTheEnd
        }
    }

    LaunchedEffect(list, isLoading) {
        snapshotFlow { isNearTheEnd }
            .distinctUntilChanged()
            .filter { it && !isLoading }
            .collect { onLoadMore() }
    }
}
