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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme

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
        topBar = { TrainingTopBar(title = "True or False", onClose = onClose) },
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
            is TrueOrFalseUiState.Loaded ->
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
                            onClick = { onAnswer(true) },
                            enabled = uiState.isEditable,
                            colors = buttonColorsFor(selected = uiState.userAnsweredTrue == true, uiState = uiState),
                        ) {
                            Text("True")
                        }
                        Button(
                            onClick = { onAnswer(false) },
                            enabled = uiState.isEditable,
                            colors = buttonColorsFor(selected = uiState.userAnsweredTrue == false, uiState = uiState),
                        ) {
                            Text("False")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    ) {
                        TextButton(onClick = onSkip, enabled = uiState.canSkip) {
                            Text("Skip")
                        }
                        if (uiState.awaitingNext) {
                            Button(onClick = onNext) {
                                Text("Next")
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
                    totalSteps = 10,
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
