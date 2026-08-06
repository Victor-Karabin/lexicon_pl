package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens

/**
 * Shared Undo/Tip/Skip/Check-or-Next action row for the Tip-capable trainings (Dictation,
 * Dictation Puzzle, Puzzle). Pass null for [onUndo], [onTip], or [onSkip] to hide that button.
 */
@Composable
fun TrainingActionRow(
    onCheck: () -> Unit,
    onNext: () -> Unit,
    awaitingNext: Boolean,
    checkEnabled: Boolean,
    onUndo: (() -> Unit)? = null,
    onTip: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(Dimens.spacingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall, Alignment.End),
    ) {
        onUndo?.let { undo ->
            TextButton(onClick = debounced(onClick = undo)) {
                Text(stringResource(R.string.action_undo))
            }
        }
        onTip?.let { tip ->
            TextButton(onClick = debounced(onClick = tip)) {
                Text(stringResource(R.string.action_tip))
            }
        }
        onSkip?.let { skip ->
            TextButton(onClick = debounced(onClick = skip)) {
                Text(stringResource(R.string.action_skip))
            }
        }
        Button(
            onClick =
                debounced {
                    if (awaitingNext) onNext() else onCheck()
                },
            enabled = awaitingNext || checkEnabled,
        ) {
            Text(stringResource(if (awaitingNext) R.string.action_next else R.string.action_check))
        }
    }
}
