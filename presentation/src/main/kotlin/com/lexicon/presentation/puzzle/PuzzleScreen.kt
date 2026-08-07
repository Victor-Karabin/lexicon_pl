package com.lexicon.presentation.puzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
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

private val ImageHeight = 180.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PuzzleViewModel = hiltViewModel(),
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

    PuzzleScreenContent(
        uiState = uiState,
        onClose = onClose,
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
private fun PuzzleScreenContent(
    uiState: PuzzleUiState,
    onClose: () -> Unit,
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
        topBar = { TrainingTopBar(title = stringResource(R.string.puzzle_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is PuzzleUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is PuzzleUiState.Loaded -> {
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
                        LinearProgressIndicator(
                            progress = { (uiState.stepIndex + 1f) / uiState.totalSteps },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${uiState.stepIndex + 1} / ${uiState.totalSteps}",
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.labelMedium,
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(ImageHeight).padding(top = Dimens.spacingMedium)) {
                            if (uiState.imageUrl == null) {
                                Text(uiState.clueText, style = MaterialTheme.typography.headlineSmall)
                            } else {
                                SubcomposeAsyncImage(
                                    model = uiState.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    },
                                    error = { Text(uiState.clueText, style = MaterialTheme.typography.headlineSmall) },
                                )
                            }
                        }

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

                        if (uiState.isEditable) {
                            uiState.tipTranslation?.let { hint ->
                                Text(
                                    text = stringResource(R.string.hint_format, hint),
                                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
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
private fun PuzzleScreenUnansweredPreview() {
    LexiconTheme {
        PuzzleScreenContent(
            uiState =
                PuzzleUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    clueText = "praca",
                    stepTiles = previewTiles,
                    placedTiles = previewTiles.take(2),
                ),
            onClose = {},
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
private fun PuzzleScreenIncorrectPreview() {
    LexiconTheme {
        PuzzleScreenContent(
            uiState =
                PuzzleUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    clueText = "praca",
                    stepTiles = previewTiles,
                    placedTiles = previewTiles,
                    answerState = AnswerState.Incorrect("praca"),
                ),
            onClose = {},
            onUndo = {},
            onTileSelected = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
