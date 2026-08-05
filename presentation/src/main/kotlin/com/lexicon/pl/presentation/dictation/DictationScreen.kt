package com.lexicon.pl.presentation.dictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.pl.presentation.theme.Dimens
import com.lexicon.pl.presentation.theme.LexiconError
import com.lexicon.pl.presentation.theme.LexiconSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictationScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is DictationNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Dictation") }) },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
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

            TextButton(onClick = viewModel::onReplayAudio, modifier = Modifier.padding(top = Dimens.spacingLarge)) {
                Text("🔊 Listen again")
            }

            val fieldColors = when (uiState.answerState) {
                DictationAnswerState.CORRECT -> OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LexiconSuccess,
                    unfocusedBorderColor = LexiconSuccess,
                )
                DictationAnswerState.INCORRECT, DictationAnswerState.SKIPPED -> OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LexiconError,
                    unfocusedBorderColor = LexiconError,
                )
                DictationAnswerState.UNANSWERED -> OutlinedTextFieldDefaults.colors()
            }

            OutlinedTextField(
                value = uiState.answerText,
                onValueChange = viewModel::onAnswerChanged,
                enabled = uiState.isEditable,
                readOnly = !uiState.isEditable,
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
                label = { Text("Type what you heard") },
            )

            uiState.revealedAnswer?.let { answer ->
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
                TextButton(onClick = viewModel::onTipRequested, enabled = uiState.canUseTip) {
                    Text("Tip")
                }
                TextButton(onClick = viewModel::onSkip, enabled = uiState.canSkip) {
                    Text("Skip")
                }
                Button(
                    onClick = { if (uiState.awaitingNext) viewModel.onNext() else viewModel.onCheck() },
                    enabled = uiState.awaitingNext || uiState.canCheck,
                ) {
                    Text(if (uiState.awaitingNext) "Next" else "Check")
                }
            }
        }
    }
}
