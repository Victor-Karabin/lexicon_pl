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

/** Fraction of the reveal that has to be dragged for it to settle open rather than snap back. */
private const val REVEAL_THRESHOLD = 0.4f

/** A flick faster than this settles the row regardless of how far it travelled, in px/second. */
private const val FLING_VELOCITY = 400f

/**
 * Slides its foreground aside to reveal something behind it — a row's actions, without those
 * actions taking up room in the row.
 *
 * Deliberately a reveal rather than a swipe-to-dismiss: the gesture uncovers a button and the
 * button does the work, so a destructive action still takes a deliberate tap. A swipe that
 * deleted on its own would make an accidental brush unrecoverable.
 *
 * Built on `draggable` over a plain `Animatable` rather than `AnchoredDraggableState`. The
 * offset here is only ever a number between two known values, and the anchored API owns its own
 * initialisation — its offset is unset until anchors have been applied in layout. A row whose
 * offset never leaves that state simply never moves: nothing is drawn wrong and nothing is
 * thrown, the swipe just does nothing, which is the hardest kind of failure to see.
 */
@Composable
fun SwipeToRevealContainer(
    revealWidth: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Collapses the row from outside — after the revealed action has been taken, say. */
    collapseSignal: Any? = null,
    backgroundContent: @Composable BoxScope.() -> Unit,
    foregroundContent: @Composable () -> Unit,
) {
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    val scope = rememberCoroutineScope()

    // Negative: the foreground travels left, uncovering the background on the right.
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(collapseSignal) {
        if (collapseSignal != null) offsetX.animateTo(0f)
    }

    // The box takes its height from the foreground, and the background is matched to it.
    // Not IntrinsicSize.Min with a fillMaxHeight child: that asks every modifier in the
    // foreground to answer an intrinsic-height query, and any that answers zero collapses the
    // background to nothing — invisible, with the row above it still looking perfectly normal.
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
                // Opaque, or the action behind shows through the row still covering it.
                .background(MaterialTheme.colorScheme.background)
                // Swallows taps so a press on the row cannot reach the action behind it.
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
