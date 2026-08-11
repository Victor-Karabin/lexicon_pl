package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.course.GapFillItem
import com.lexicon.interactors.course.MinimalPairItem
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.answerStateColor
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes

private val PlayIconSize = 20.dp

/** The gap marker the extractor writes into a prompt. */
const val GAP_MARKER = "___"

@Composable
fun ExerciseAudioButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(PlayIconSize),
        )
        Text(
            text = stringResource(if (isPlaying) R.string.exercise_pause else R.string.exercise_play),
            modifier = Modifier.padding(start = Dimens.spacingSmall),
        )
    }
}

/**
 * One of two near-identical words. Which was said is only knowable from the
 * recording, so nothing is revealed until the learner has committed to a choice.
 */
@Composable
fun MinimalPairRow(
    item: MinimalPairItem,
    selected: String?,
    answerState: AnswerState,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Text(
            text = "${item.label})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        item.options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                enabled = answerState is AnswerState.Unanswered,
                label = { Text(option) },
            )
        }
    }
}

@Composable
fun GapFillRow(
    item: GapFillItem,
    values: List<String>,
    answerState: AnswerState,
    onValueChanged: (Int, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall)) {
        Text(
            text = item.prompt,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        item.answers.forEachIndexed { index, expected ->
            OutlinedTextField(
                value = values.getOrElse(index) { "" },
                onValueChange = { onValueChanged(index, it) },
                singleLine = true,
                enabled = answerState is AnswerState.Unanswered,
                isError = answerState is AnswerState.Incorrect,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmall),
                shape = LexiconShapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                supportingText = {
                    if (answerState !is AnswerState.Unanswered) {
                        Text(
                            text = stringResource(R.string.expected_format, expected),
                            color = answerStateColor(answerState),
                        )
                    }
                },
            )
        }
    }
}
