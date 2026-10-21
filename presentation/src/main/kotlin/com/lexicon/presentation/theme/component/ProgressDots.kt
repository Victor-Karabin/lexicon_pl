package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

@Composable
fun ProgressDots(
    step: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
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

@LightDarkPreview
@Composable
private fun ProgressDotsStatesPreview() {
    LexiconTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            ) {
                listOf(0, 4, 9).forEach { step ->
                    ProgressDots(step = step, total = 10, modifier = Modifier.fillMaxWidth())
                }
                ProgressDots(step = 0, total = 1, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
