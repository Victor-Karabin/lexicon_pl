package com.lexicon.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import kotlinx.coroutines.launch

val ScrollToTopClearance = 72.dp

@Composable
fun ScrollToTopLayout(
    list: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        ScrollToTopButton(list = list, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Dimens.spacingMedium))
    }
}

@Composable
private fun ScrollToTopButton(
    list: LazyListState,
    modifier: Modifier,
) {
    val scrolled by remember(list) { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 0 } }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = scrolled,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        SmallFloatingActionButton(onClick = { scope.launch { list.animateScrollToItem(0) } }) {
            Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.scroll_to_top))
        }
    }
}
