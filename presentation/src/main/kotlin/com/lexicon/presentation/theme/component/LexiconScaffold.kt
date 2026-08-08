package com.lexicon.presentation.theme.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

/**
 * Shared training-step shell: close + title, progress slot, content slot, action row slot.
 * Formalizes the Scaffold + TrainingTopBar structure duplicated across every training screen —
 * not yet wired into any of them; each screen still builds this shape inline.
 */
@Composable
fun LexiconScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    progress: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            progress?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
                ) {
                    it()
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMedium),
                content = content,
            )
            actions?.let {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                    horizontalArrangement = Arrangement.End,
                    content = it,
                )
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun LexiconScaffoldPreview() {
    LexiconTheme {
        LexiconScaffold(
            title = "Dictation",
            onClose = {},
            progress = { ProgressDots(step = 2, total = 10, modifier = Modifier.fillMaxWidth()) },
            actions = {
                TextButton(onClick = {}) { Text("Skip") }
                Button(onClick = {}) { Text("Check") }
            },
        ) {
            WordCard(word = "praca", sublabel = "work", modifier = Modifier.padding(top = Dimens.spacingLarge))
        }
    }
}

/** The slots are optional; this is the bare shell with neither progress nor actions. */
@LightDarkPreview
@Composable
private fun LexiconScaffoldContentOnlyPreview() {
    LexiconTheme {
        LexiconScaffold(title = "Crossword", onClose = {}) {
            Text("Content only", modifier = Modifier.padding(top = Dimens.spacingLarge))
        }
    }
}
