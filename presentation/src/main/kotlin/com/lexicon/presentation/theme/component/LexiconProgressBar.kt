package com.lexicon.presentation.theme.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LexiconProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        gapSize = gapSize,
        drawStopIndicator = {},
    )
}

@LightDarkPreview
@Composable
private fun LexiconProgressBarPreview() {
    LexiconTheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            ) {
                LexiconProgressBar(progress = { 0f }, modifier = Modifier.fillMaxWidth())
                LexiconProgressBar(progress = { 0.35f }, modifier = Modifier.fillMaxWidth())
                LexiconProgressBar(progress = { 1f }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
