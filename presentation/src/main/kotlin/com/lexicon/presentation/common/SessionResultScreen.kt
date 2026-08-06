package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.LexiconWarning
import kotlin.math.roundToInt

/** Shared end-of-session summary, reused by every training's result screen. */
@Composable
fun SessionResultScreen(
    correct: Int,
    incorrect: Int,
    skipped: Int,
    tipsUsed: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionResultViewModel = hiltViewModel(),
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

@OptIn(ExperimentalMaterial3Api::class)
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
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "$accuracyPercent% accuracy",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (wordResults.isNotEmpty()) {
                item { HorizontalDivider(modifier = Modifier.padding(top = Dimens.spacingMedium)) }
                items(wordResults) { entry ->
                    WordResultRow(entry, modifier = Modifier.padding(vertical = Dimens.spacingSmall))
                    HorizontalDivider()
                }
            } else {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge)) {
                        CountRow(label = "Correct", value = correct, color = LexiconSuccess)
                        CountRow(label = "Incorrect", value = incorrect, color = LexiconError)
                        CountRow(label = "Skipped", value = skipped, color = LexiconWarning)
                        CountRow(label = "Tips used", value = tipsUsed, color = MaterialTheme.colorScheme.outline)
                    }
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
    entry: WordResultEntry,
    modifier: Modifier = Modifier,
) {
    // Tip used takes over the status column entirely, rather than sitting alongside Correct/Incorrect.
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
        Column {
            Text(entry.word, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                entry.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(statusLabel, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CountRow(
    label: String,
    value: Int,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingTiny),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text("$value", style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Bold)
    }
}

private val previewWordResults = listOf(
    WordResultEntry(word = "praca", translation = "work", outcome = AnswerState.Correct),
    WordResultEntry(word = "dom", translation = "house", outcome = AnswerState.Incorrect()),
    WordResultEntry(word = "kot", translation = "cat", outcome = AnswerState.Skipped()),
    WordResultEntry(word = "pies", translation = "dog", outcome = AnswerState.Correct, tipUsed = true),
)

@Preview(showBackground = true)
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

@Preview(showBackground = true)
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
