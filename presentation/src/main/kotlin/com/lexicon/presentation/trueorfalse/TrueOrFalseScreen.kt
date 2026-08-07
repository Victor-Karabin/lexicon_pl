package com.lexicon.presentation.trueorfalse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme

private const val LOW_TIME_WARNING_SECONDS = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrueOrFalseScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrueOrFalseViewModel = hiltViewModel(),
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

    TrueOrFalseScreenContent(
        uiState = uiState,
        onClose = onClose,
        onAnswer = viewModel::onAnswer,
        onSkip = viewModel::onSkip,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrueOrFalseScreenContent(
    uiState: TrueOrFalseUiState,
    onClose: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.true_or_false_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is TrueOrFalseUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is TrueOrFalseUiState.Loaded -> {
                val timerColor = if (uiState.timeRemainingSeconds <= LOW_TIME_WARNING_SECONDS) {
                    LexiconError
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(Dimens.spacingMedium),
                    ) {
                        LinearProgressIndicator(
                            progress = { uiState.timeRemainingSeconds / TRUE_OR_FALSE_TIME_LIMIT_SECONDS.toFloat() },
                            color = timerColor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.time_remaining_format, uiState.timeRemainingSeconds),
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.labelMedium,
                            color = timerColor,
                        )

                        Text(
                            text = uiState.word,
                            modifier = Modifier.padding(top = Dimens.spacingLarge),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = uiState.displayedTranslation,
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                        ) {
                            Button(
                                onClick = debounced { onAnswer(true) },
                                enabled = uiState.isEditable,
                                colors = buttonColorsFor(selected = uiState.userAnsweredTrue == true, uiState = uiState),
                            ) {
                                Text(stringResource(R.string.action_true))
                            }
                            Button(
                                onClick = debounced { onAnswer(false) },
                                enabled = uiState.isEditable,
                                colors = buttonColorsFor(selected = uiState.userAnsweredTrue == false, uiState = uiState),
                            ) {
                                Text(stringResource(R.string.action_false))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall, Alignment.End),
                    ) {
                        if (uiState.canSkip) {
                            TextButton(onClick = debounced(onClick = onSkip)) {
                                Text(stringResource(R.string.action_skip))
                            }
                        }
                        if (uiState.awaitingNext) {
                            Button(onClick = debounced(onClick = onNext)) {
                                Text(stringResource(R.string.action_next))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun buttonColorsFor(
    selected: Boolean,
    uiState: TrueOrFalseUiState.Loaded,
) = when {
    !selected -> ButtonDefaults.buttonColors()
    uiState.answerState is AnswerState.Correct -> ButtonDefaults.buttonColors(containerColor = LexiconSuccess)
    else -> ButtonDefaults.buttonColors(containerColor = LexiconError)
}

@Preview(showBackground = true)
@Composable
private fun TrueOrFalseScreenPreview() {
    LexiconTheme {
        TrueOrFalseScreenContent(
            uiState =
                TrueOrFalseUiState.Loaded(
                    stepIndex = 2,
                    timeRemainingSeconds = 42,
                    word = "praca",
                    displayedTranslation = "work",
                ),
            onClose = {},
            onAnswer = {},
            onSkip = {},
            onNext = {},
        )
    }
}
