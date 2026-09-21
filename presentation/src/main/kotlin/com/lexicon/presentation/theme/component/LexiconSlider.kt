package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val ThumbSize = 20.dp
private val TrackHeight = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LexiconSlider(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = value.coerceIn(range).toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(ThumbSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                modifier = Modifier.height(TrackHeight),
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null,
            )
        },
    )
}
