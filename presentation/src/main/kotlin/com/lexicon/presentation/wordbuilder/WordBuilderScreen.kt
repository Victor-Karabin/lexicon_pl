package com.lexicon.presentation.wordbuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.LetterTileGrid
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.shuffleIntoTiles
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordBuilderScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordBuilderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    WordBuilderScreenContent(
        uiState = uiState,
        onClose = onClose,
        onAnswerFieldCleared = viewModel::onAnswerFieldCleared,
        onTileSelected = viewModel::onTileSelected,
        onTipRequested = viewModel::onTipRequested,
        onSkip = viewModel::onSkip,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordBuilderScreenContent(
    uiState: WordBuilderUiState,
    onClose: () -> Unit,
    onAnswerFieldCleared: () -> Unit,
    onTileSelected: (LetterTile) -> Unit,
    onTipRequested: () -> Unit,
    onSkip: () -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = "Word Builder", onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is WordBuilderUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is WordBuilderUiState.Loaded -> {
                val answerColor = when (uiState.answerState) {
                    is AnswerState.Correct -> LexiconSuccess
                    is AnswerState.Incorrect, is AnswerState.Skipped -> LexiconError
                    is AnswerState.Unanswered -> MaterialTheme.colorScheme.outline
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium)) {
                    LinearProgressIndicator(
                        progress = { (uiState.stepIndex + 1f) / uiState.totalSteps },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${uiState.stepIndex + 1} / ${uiState.totalSteps}",
                        modifier = Modifier.padding(top = Dimens.spacingSmall),
                        style = MaterialTheme.typography.labelMedium,
                    )

                    Text(
                        text = uiState.clueText,
                        modifier = Modifier.padding(top = Dimens.spacingLarge),
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.spacingMedium)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.spacingSmall))
                            .clickable(enabled = uiState.isEditable, onClick = onAnswerFieldCleared)
                            .padding(Dimens.spacingMedium),
                    ) {
                        Text(
                            text = uiState.builtAnswer.ifEmpty { " " },
                            style = MaterialTheme.typography.headlineSmall,
                            color = answerColor,
                        )
                    }

                    LetterTileGrid(
                        tiles = uiState.availableTiles,
                        onTileSelected = onTileSelected,
                        modifier = Modifier.padding(top = Dimens.spacingMedium),
                    )

                    (uiState.revealedAnswer ?: uiState.tipTranslation)?.let { answer ->
                        Text(
                            text = "Expected: $answer",
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    ) {
                        TextButton(onClick = onTipRequested, enabled = uiState.canUseTip) {
                            Text("Tip")
                        }
                        TextButton(onClick = onSkip, enabled = uiState.canSkip) {
                            Text("Skip")
                        }
                        Button(
                            onClick = { if (uiState.awaitingNext) onNext() else onCheck() },
                            enabled = uiState.awaitingNext || uiState.canCheck,
                        ) {
                            Text(if (uiState.awaitingNext) "Next" else "Check")
                        }
                    }
                }
            }
        }
    }
}

private val previewTiles = shuffleIntoTiles("praca")

@Preview(showBackground = true)
@Composable
private fun WordBuilderScreenPreview() {
    LexiconTheme {
        WordBuilderScreenContent(
            uiState =
                WordBuilderUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    clueText = "work",
                    stepTiles = previewTiles,
                    placedTiles = previewTiles.take(2),
                ),
            onClose = {},
            onAnswerFieldCleared = {},
            onTileSelected = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
