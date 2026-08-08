package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

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

@LightDarkPreview
@Composable
private fun TrainingActionRowStatesPreview() {
    LexiconTheme {
        Surface {
            Column(modifier = Modifier.padding(vertical = Dimens.spacingSmall)) {
                Label("all actions, Check enabled")
                TrainingActionRow(
                    onCheck = {},
                    onNext = {},
                    awaitingNext = false,
                    checkEnabled = true,
                    onUndo = {},
                    onTip = {},
                    onSkip = {},
                )

                Label("Check disabled (nothing entered yet)")
                TrainingActionRow(onCheck = {}, onNext = {}, awaitingNext = false, checkEnabled = false, onSkip = {})

                Label("awaiting Next after an incorrect answer")
                TrainingActionRow(onCheck = {}, onNext = {}, awaitingNext = true, checkEnabled = false)

                Label("Tip already used, so only Skip remains")
                TrainingActionRow(onCheck = {}, onNext = {}, awaitingNext = false, checkEnabled = true, onSkip = {})

                Label("Check only (no Tip, Skip or Undo)")
                TrainingActionRow(onCheck = {}, onNext = {}, awaitingNext = false, checkEnabled = true)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = Dimens.spacingMedium),
    )
}
