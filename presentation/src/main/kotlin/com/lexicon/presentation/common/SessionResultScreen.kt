package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.LexiconWarning
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.StatChip
import com.lexicon.presentation.theme.component.TileChips
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SessionResultScreen(
    correct: Int,
    incorrect: Int,
    skipped: Int,
    tipsUsed: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionResultViewModel = koinViewModel(),
) {
    SessionResultScreenContent(
        correct = correct,
        incorrect = incorrect,
        skipped = skipped,
        tipsUsed = tipsUsed,
        wordResults = viewModel.wordResults,
        onDone = onDone,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SessionResultScreenContent(
    correct: Int,
    incorrect: Int,
    skipped: Int,
    tipsUsed: Int,
    wordResults: List<WordResultEntry>,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalSteps = correct + incorrect + skipped
    val accuracyPercent = if (totalSteps > 0) (correct * 100f / totalSteps).roundToInt() else 0

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = {}) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Dimens.spacingLarge),
        ) {
            item {
                val skin = tileSkin(highlighted = true)
                GradientTile(skin = skin, modifier = Modifier.padding(top = Dimens.spacingSmall)) {
                    Text(
                        text = "$accuracyPercent%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = skin.onTile,
                    )
                    Text(
                        text = stringResource(R.string.result_accuracy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = skin.muted(),
                    )
                    TileChips {
                        StatChip(
                            icon = Icons.Default.Check,
                            text = "$correct",
                            skin = skin,
                        )
                        if (incorrect > 0) {
                            StatChip(icon = Icons.Default.Close, text = "$incorrect", skin = skin)
                        }
                        if (skipped > 0) {
                            StatChip(icon = Icons.AutoMirrored.Filled.ArrowForward, text = "$skipped", skin = skin)
                        }
                        if (tipsUsed > 0) {
                            StatChip(icon = Icons.Default.Lightbulb, text = "$tipsUsed", skin = skin)
                        }
                    }
                }
            }

            if (wordResults.isNotEmpty()) {
                item { HorizontalDivider(modifier = Modifier.padding(top = Dimens.spacingMedium)) }
                itemsIndexed(wordResults) { index, entry ->
                    WordResultRow(index + 1, entry, modifier = Modifier.padding(vertical = Dimens.spacingSmall))
                    HorizontalDivider()
                }
            }

            item {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingLarge),
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun WordResultRow(
    order: Int,
    entry: WordResultEntry,
    modifier: Modifier = Modifier,
) {
    val color = when {
        entry.tipUsed -> MaterialTheme.colorScheme.outline
        entry.outcome is AnswerState.Correct -> LexiconSuccess
        entry.outcome is AnswerState.Incorrect -> LexiconError
        entry.outcome is AnswerState.Skipped -> LexiconWarning
        else -> MaterialTheme.colorScheme.outline
    }
    val statusLabel = if (entry.tipUsed) {
        "Tip used"
    } else {
        when (entry.outcome) {
            is AnswerState.Correct -> "Correct"
            is AnswerState.Incorrect -> "Incorrect"
            is AnswerState.Skipped -> "Skipped"
            is AnswerState.Unanswered -> ""
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$order.",
                modifier = Modifier.padding(end = Dimens.spacingSmall),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Text(entry.word, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    entry.translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(statusLabel, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

private val previewWordResults = listOf(
    WordResultEntry(word = "praca", translation = "work", outcome = AnswerState.Correct),
    WordResultEntry(word = "dom", translation = "house", outcome = AnswerState.Incorrect()),
    WordResultEntry(word = "kot", translation = "cat", outcome = AnswerState.Skipped()),
    WordResultEntry(word = "pies", translation = "dog", outcome = AnswerState.Correct, tipUsed = true),
)

@LightDarkPreview
@Composable
private fun SessionResultScreenWordListPreview() {
    LexiconTheme {
        SessionResultScreenContent(
            correct = 2,
            incorrect = 1,
            skipped = 1,
            tipsUsed = 1,
            wordResults = previewWordResults,
            onDone = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun SessionResultScreenCountsOnlyPreview() {
    LexiconTheme {
        SessionResultScreenContent(
            correct = 6,
            incorrect = 2,
            skipped = 1,
            tipsUsed = 1,
            wordResults = emptyList(),
            onDone = {},
        )
    }
}
