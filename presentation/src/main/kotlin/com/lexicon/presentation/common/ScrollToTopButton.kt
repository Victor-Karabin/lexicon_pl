package com.lexicon.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import kotlinx.coroutines.launch

val ScrollToTopClearance = 72.dp

@Composable
fun ScrollToTopButton(
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scrolled by remember(scroll) { derivedStateOf { scroll.value > 0 } }
    val scope = rememberCoroutineScope()
    ScrollToTopButton(visible = enabled && scrolled, onClick = { scope.launch { scroll.animateScrollTo(0) } }, modifier = modifier)
}

@Composable
fun ScrollToTopButton(
    list: LazyListState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scrolled by remember(list) { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 0 } }
    val scope = rememberCoroutineScope()
    ScrollToTopButton(visible = enabled && scrolled, onClick = { scope.launch { list.animateScrollToItem(0) } }, modifier = modifier)
}

@Composable
private fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        SmallFloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.scroll_to_top))
        }
    }
}
