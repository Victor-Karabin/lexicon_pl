package com.lexicon.pl.presentation.dictationpuzzle

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.pl.presentation.common.AnswerState
import com.lexicon.pl.presentation.common.LetterTileGrid
import com.lexicon.pl.presentation.common.SessionNavigationEvent
import com.lexicon.pl.presentation.theme.Dimens
import com.lexicon.pl.presentation.theme.LexiconError
import com.lexicon.pl.presentation.theme.LexiconSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictationPuzzleScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictationPuzzleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Dictation Puzzle") }) },
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

        val answerColor =
            when (uiState.answerState) {
                AnswerState.CORRECT -> LexiconSuccess
                AnswerState.INCORRECT, AnswerState.SKIPPED -> LexiconError
                AnswerState.UNANSWERED -> MaterialTheme.colorScheme.outline
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

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.spacingMedium)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.spacingSmall))
                        .clickable(enabled = uiState.isEditable, onClick = viewModel::onAnswerFieldCleared)
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
                onTileSelected = viewModel::onTileSelected,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
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
