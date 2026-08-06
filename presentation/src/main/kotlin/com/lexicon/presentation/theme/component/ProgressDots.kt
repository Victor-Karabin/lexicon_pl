package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.theme.Dimens

enum class ProgressDotsVariant { BAR, DOTS }

/**
 * Session progress, shown under the title bar on every multi-step training (not Crossword).
 * [ProgressDotsVariant.BAR] matches the current app (LinearProgressIndicator-style bar + counter);
 * [ProgressDotsVariant.DOTS] is DESIGN.md §8.3's bubble-mode treatment, not yet used anywhere.
 */
@Composable
fun ProgressDots(
    step: Int,
    total: Int,
    modifier: Modifier = Modifier,
    variant: ProgressDotsVariant = ProgressDotsVariant.BAR,
) {
    when (variant) {
        ProgressDotsVariant.DOTS ->
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(total) { index ->
                    val filled = index <= step
                    val dotSize = if (index == step) 10.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .then(
                                if (filled) {
                                    Modifier.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                },
                            ),
                    )
                }
            }

        ProgressDotsVariant.BAR ->
            Column(modifier = modifier) {
                val barHeight = 4.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(barHeight / 2))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((step + 1f) / total)
                            .height(barHeight)
                            .clip(RoundedCornerShape(barHeight / 2))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    text = "${step + 1} / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.spacingTiny),
                )
            }
    }
}
