package com.lexicon.presentation.dictationpuzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.LetterTileGrid
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.common.shuffleIntoTiles
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.PlayButton
import com.lexicon.presentation.theme.component.ProgressDots

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictationPuzzleScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictationPuzzleViewModel = hiltViewModel(),
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

    DictationPuzzleScreenContent(
        uiState = uiState,
        onClose = onClose,
        onReplayAudio = viewModel::onReplayAudio,
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
private fun DictationPuzzleScreenContent(
    uiState: DictationPuzzleUiState,
    onClose: () -> Unit,
    onReplayAudio: () -> Unit,
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
        topBar = { TrainingTopBar(title = stringResource(R.string.dictation_puzzle_title), onClose = onClose) },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Mirrors DictationScreen's state -> color mapping so both screens read consistently.
            val answerColor = when (uiState.answerState) {
                AnswerState.CORRECT -> LexiconSuccess
                AnswerState.INCORRECT, AnswerState.SKIPPED -> LexiconError
                AnswerState.UNANSWERED -> MaterialTheme.colorScheme.outline
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(Dimens.spacingMedium),
                ) {
                    ProgressDots(
                        step = uiState.stepIndex,
                        total = uiState.totalSteps,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    PlayButton(
                        onClick = onReplayAudio,
                        label = stringResource(R.string.action_listen_again),
                        modifier = Modifier.padding(top = Dimens.spacingLarge),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.spacingMedium)
                            .clip(LexiconShapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, answerColor, LexiconShapes.small)
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

                    val statusLabel = when (uiState.answerState) {
                        AnswerState.CORRECT -> stringResource(R.string.status_correct)
                        AnswerState.INCORRECT -> stringResource(R.string.status_incorrect)
                        AnswerState.SKIPPED -> stringResource(R.string.status_skipped)
                        AnswerState.UNANSWERED -> null
                    }
                    statusLabel?.let { label ->
                        Text(
                            text = label,
                            color = answerColor,
                            modifier = Modifier.padding(top = Dimens.spacingMedium),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    uiState.revealedAnswer?.let { answer ->
                        Text(
                            text = stringResource(R.string.expected_format, answer),
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall, Alignment.End),
                ) {
                    if (uiState.canUseTip) {
                        TextButton(onClick = debounced(onClick = onTipRequested)) {
                            Text(stringResource(R.string.action_tip))
                        }
                    }
                    if (uiState.canSkip) {
                        TextButton(onClick = debounced(onClick = onSkip)) {
                            Text(stringResource(R.string.action_skip))
                        }
                    }
                    Button(
                        onClick =
                            debounced {
                                if (uiState.awaitingNext) onNext() else onCheck()
                            },
                        enabled = uiState.awaitingNext || uiState.canCheck,
                    ) {
                        Text(stringResource(if (uiState.awaitingNext) R.string.action_next else R.string.action_check))
                    }
                }
            }
        }
    }
}

private val previewTiles = shuffleIntoTiles("praca")

@Preview(showBackground = true)
@Composable
private fun DictationPuzzleScreenUnansweredPreview() {
    LexiconTheme {
        DictationPuzzleScreenContent(
            uiState =
                DictationPuzzleUiState(
                    isLoading = false,
                    stepIndex = 2,
                    totalSteps = 10,
                    stepTiles = previewTiles,
                    placedTiles = previewTiles.take(2),
                ),
            onClose = {},
            onReplayAudio = {},
            onAnswerFieldCleared = {},
            onTileSelected = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DictationPuzzleScreenIncorrectPreview() {
    LexiconTheme {
        DictationPuzzleScreenContent(
            uiState =
                DictationPuzzleUiState(
                    isLoading = false,
                    stepIndex = 2,
                    totalSteps = 10,
                    stepTiles = previewTiles,
                    placedTiles = previewTiles,
                    answerState = AnswerState.INCORRECT,
                    revealedAnswer = "praca",
                ),
            onClose = {},
            onReplayAudio = {},
            onAnswerFieldCleared = {},
            onTileSelected = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
