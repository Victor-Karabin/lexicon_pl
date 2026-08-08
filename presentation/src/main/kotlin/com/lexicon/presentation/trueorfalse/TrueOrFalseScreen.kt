package com.lexicon.presentation.trueorfalse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerTone
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.TrueOrFalseAnswerRow
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconTheme

private const val LOW_TIME_WARNING_SECONDS = 10
private val TimerSize = 100.dp

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
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrueOrFalseScreenContent(
    uiState: TrueOrFalseUiState,
    onClose: () -> Unit,
    onAnswer: (Boolean) -> Unit,
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
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium)) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(modifier = Modifier.size(TimerSize), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { uiState.timeRemainingSeconds / TRUE_OR_FALSE_TIME_LIMIT_SECONDS.toFloat() },
                                color = timerColor,
                                strokeWidth = 8.dp,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Text(
                                text = stringResource(R.string.time_remaining_format, uiState.timeRemainingSeconds),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = timerColor,
                            )
                        }

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
                    }

                    TrueOrFalseAnswerRow(
                        trueTone = AnswerTone.POSITIVE,
                        falseTone = AnswerTone.NEGATIVE,
                        enabled = uiState.isEditable,
                        onAnswer = onAnswer,
                    )
                }
            }
        }
    }
}

@LightDarkPreview
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
        )
    }
}

@LightDarkPreview
@Composable
private fun TrueOrFalseScreenLowTimePreview() {
    LexiconTheme {
        TrueOrFalseScreenContent(
            uiState =
                TrueOrFalseUiState.Loaded(
                    stepIndex = 24,
                    timeRemainingSeconds = 5,
                    word = "przyjaciel",
                    displayedTranslation = "friend",
                ),
            onClose = {},
            onAnswer = {},
        )
    }
}
