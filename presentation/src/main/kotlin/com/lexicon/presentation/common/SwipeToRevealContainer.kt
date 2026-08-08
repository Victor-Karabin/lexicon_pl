package com.lexicon.presentation.common

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class RevealState { COLLAPSED, REVEALED }

/**
 * Slides its foreground aside to reveal something behind it — a row's actions, without those
 * actions taking up room in the row.
 *
 * Deliberately a reveal rather than a swipe-to-dismiss: the gesture uncovers a button and the
 * button does the work, so a destructive action still takes a deliberate tap. A swipe that
 * deleted on its own would make an accidental brush unrecoverable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeToRevealContainer(
    revealWidth: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Collapses the row from outside — after the revealed action has been taken, say. */
    collapseSignal: Any? = null,
    backgroundContent: @Composable () -> Unit,
    foregroundContent: @Composable () -> Unit,
) {
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    val density = LocalDensity.current
    val state = remember(revealPx) {
        AnchoredDraggableState(
            initialValue = RevealState.COLLAPSED,
            anchors = DraggableAnchors {
                RevealState.COLLAPSED at 0f
                // Negative: the foreground moves left, uncovering the background on the right.
                RevealState.REVEALED at -revealPx
            },
            positionalThreshold = { distance -> distance * REVEAL_THRESHOLD },
            velocityThreshold = { with(density) { VelocityThreshold.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    LaunchedEffect(collapseSignal) {
        if (collapseSignal != null) state.animateTo(RevealState.COLLAPSED)
    }

    Box(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Sized and aligned to the reveal, so the action fills exactly what the gesture opens
        // rather than stretching behind the whole row.
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(modifier = Modifier.width(revealWidth).fillMaxHeight()) { backgroundContent() }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .fillMaxWidth()
                .wrapContentHeight()
                .anchoredDraggable(state, Orientation.Horizontal, enabled = enabled)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.CenterStart,
        ) {
            foregroundContent()
        }
    }
}

/** Fraction of the reveal that has to be crossed for the gesture to settle open. */
private const val REVEAL_THRESHOLD = 0.4f

/** A flick faster than this opens regardless of how far it travelled. */
private val VelocityThreshold = 125.dp
