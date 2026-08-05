package com.lexicon.pl.presentation.wordmatch

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
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.pl.presentation.common.SessionNavigationEvent
import com.lexicon.pl.presentation.theme.Dimens
import com.lexicon.pl.presentation.theme.LexiconError
import com.lexicon.pl.presentation.theme.LexiconSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordMatchScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordMatchViewModel = hiltViewModel(),
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
        topBar = { TopAppBar(title = { Text("Word Match") }) },
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

            Row(modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium)) {
                Column(modifier = Modifier.weight(1f)) {
                    uiState.leftColumn.forEach { item ->
                        MatchTile(
                            text = item.text,
                            state = tileState(item.vocabularyItemId, uiState.selectedLeftId, uiState),
                            onClick = { viewModel.onLeftSelected(item.vocabularyItemId) },
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = Dimens.spacingSmall)) {
                    uiState.rightColumn.forEach { item ->
                        MatchTile(
                            text = item.text,
                            state = tileState(item.vocabularyItemId, uiState.selectedRightId, uiState),
                            onClick = { viewModel.onRightSelected(item.vocabularyItemId) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                TextButton(onClick = viewModel::onSkip, enabled = uiState.canSkip) {
                    Text("Skip")
                }
                if (uiState.awaitingNext) {
                    Button(onClick = viewModel::onNext) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

private enum class MatchTileState { DEFAULT, SELECTED, MATCHED, INCORRECT }

private fun tileState(
    itemId: Long,
    selectedId: Long?,
    uiState: WordMatchUiState,
): MatchTileState =
    when {
        uiState.matchedIds.contains(itemId) -> MatchTileState.MATCHED
        uiState.incorrectFlashIds.contains(itemId) -> MatchTileState.INCORRECT
        selectedId == itemId -> MatchTileState.SELECTED
        else -> MatchTileState.DEFAULT
    }

@Composable
private fun MatchTile(
    text: String,
    state: MatchTileState,
    onClick: () -> Unit,
) {
    val background =
        when (state) {
            MatchTileState.MATCHED -> LexiconSuccess
            MatchTileState.INCORRECT -> LexiconError
            MatchTileState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
            MatchTileState.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
        }
    val enabled = state == MatchTileState.DEFAULT || state == MatchTileState.SELECTED
    val textColor =
        when (state) {
            MatchTileState.MATCHED, MatchTileState.INCORRECT -> Color.White
            MatchTileState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
            MatchTileState.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.tileVerticalSpacing)
                .background(background, RoundedCornerShape(Dimens.spacingSmall))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(Dimens.spacingSmall),
    ) {
        Text(text, color = textColor)
    }
}
