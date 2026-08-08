package com.lexicon.presentation.dictationpuzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.LetterTileGrid
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
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
        onUndo = viewModel::onUndo,
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
    onUndo: () -> Unit,
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
        when (uiState) {
            is DictationPuzzleUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is DictationPuzzleUiState.Loaded -> {
                // Mirrors DictationScreen's state -> color mapping so both screens read consistently.
                val answerColor = when (uiState.answerState) {
                    is AnswerState.Correct -> LexiconSuccess
                    is AnswerState.Incorrect, is AnswerState.Skipped -> LexiconError
                    is AnswerState.Unanswered -> MaterialTheme.colorScheme.outline
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

                        if (uiState.isEditable) {
                            uiState.tipTranslation?.let { hint ->
                                Text(
                                    text = stringResource(R.string.hint_format, hint),
                                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        val statusLabel = when (uiState.answerState) {
                            is AnswerState.Correct -> stringResource(R.string.status_correct)
                            is AnswerState.Incorrect -> stringResource(R.string.status_incorrect)
                            is AnswerState.Skipped -> stringResource(R.string.status_skipped)
                            is AnswerState.Unanswered -> null
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

                    TrainingActionRow(
                        onCheck = onCheck,
                        onNext = onNext,
                        awaitingNext = uiState.awaitingNext,
                        checkEnabled = uiState.canCheck,
                        onUndo = onUndo.takeIf { uiState.canUndo },
                        onTip = onTipRequested.takeIf { uiState.canUseTip },
                        onSkip = onSkip.takeIf { uiState.canSkip },
                    )
                }
            }
        }
    }
}

private val previewTiles = shuffleIntoTiles("praca")

@LightDarkPreview
@Composable
private fun DictationPuzzleScreenUnansweredPreview() {
    LexiconTheme {
        DictationPuzzleScreenContent(
            uiState =
                DictationPuzzleUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    stepTiles = previewTiles,
                    placedTiles = previewTiles.take(2),
                ),
            onClose = {},
            onReplayAudio = {},
            onUndo = {},
            onTileSelected = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun DictationPuzzleScreenIncorrectPreview() {
    LexiconTheme {
        DictationPuzzleScreenContent(
            uiState =
                DictationPuzzleUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    stepTiles = previewTiles,
                    placedTiles = previewTiles,
                    answerState = AnswerState.Incorrect("praca"),
                ),
            onClose = {},
            onReplayAudio = {},
            onUndo = {},
            onTileSelected = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
