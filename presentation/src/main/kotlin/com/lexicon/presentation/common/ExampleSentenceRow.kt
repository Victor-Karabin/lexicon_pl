package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens

private val PlayIconSize = 20.dp

@Composable
fun ExampleSentenceRow(
    example: ExampleSentence,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    if (example.isBlank) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = example.annotated(),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onPlay) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.example_play),
                tint = color,
                modifier = Modifier.size(PlayIconSize),
            )
        }
    }
}

@Composable
fun ExampleSentence.annotated(): AnnotatedString {
    val sentence = this
    return remember(sentence) {
        buildAnnotatedString {
            append(sentence.text)
            sentence.emphasis.forEach { range ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first, range.last + 1)
            }
        }
    }
}
