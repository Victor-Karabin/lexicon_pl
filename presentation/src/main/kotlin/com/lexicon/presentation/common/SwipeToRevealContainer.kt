package com.lexicon.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val REVEAL_THRESHOLD = 0.4f

private const val FLING_VELOCITY = 400f

@Composable
fun SwipeToRevealContainer(
    revealWidth: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    collapseSignal: Any? = null,
    backgroundContent: @Composable BoxScope.() -> Unit,
    foregroundContent: @Composable () -> Unit,
) {
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    val scope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(collapseSignal) {
        if (collapseSignal != null) offsetX.animateTo(0f)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier.width(revealWidth).fillMaxHeight(),
                content = backgroundContent,
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        val draggedPastThreshold = -offsetX.value > revealPx * REVEAL_THRESHOLD
                        val target = when {
                            velocity > FLING_VELOCITY -> 0f
                            velocity < -FLING_VELOCITY || draggedPastThreshold -> -revealPx
                            else -> 0f
                        }
                        offsetX.animateTo(target)
                    },
                )
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            foregroundContent()
        }
    }
}
