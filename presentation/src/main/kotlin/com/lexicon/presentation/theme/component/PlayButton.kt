package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

/**
 * Audio replay control — the one fully-circular element allowed outside avatars (DESIGN.md §5).
 * Same copy/behavior as the plain TextButton it refines; used in Dictation, Dictation Puzzle, and
 * Pronunciation Check.
 */
@Composable
fun PlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Listen again",
    playing: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)) // fully rounded pill for this chip's height; not a named shape-scale token
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = debounced(onClick = onClick))
            .padding(
                PaddingValues(
                    start = Dimens.spacingSmall,
                    top = Dimens.spacingSmall,
                    bottom = Dimens.spacingSmall,
                    end = Dimens.spacingMedium,
                ),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (playing) "❙❙" else "▶",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@LightDarkPreview
@Composable
private fun PlayButtonStatesPreview() {
    LexiconTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                PlayButton(onClick = {}, label = "Listen again")
                PlayButton(onClick = {}, label = "Listen again", playing = true)
                // Longer label, to check the pill grows rather than clipping.
                PlayButton(onClick = {}, label = "Play my recording")
            }
        }
    }
}
