package com.lexicon.presentation.wordmatch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordMatchScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordMatchViewModel = hiltViewModel(),
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

    WordMatchScreenContent(
        uiState = uiState,
        onClose = onClose,
        onLeftSelected = viewModel::onLeftSelected,
        onRightSelected = viewModel::onRightSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordMatchScreenContent(
    uiState: WordMatchUiState,
    onClose: () -> Unit,
    onLeftSelected: (Long) -> Unit,
    onRightSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.word_match_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is WordMatchUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is WordMatchUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium)) {
                    val leftNumbers = uiState.leftColumn.mapIndexed { index, item -> item.vocabularyItemId to index + 1 }.toMap()

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            uiState.leftColumn.forEachIndexed { index, item ->
                                MatchTile(
                                    text = item.text,
                                    number = index + 1,
                                    state = tileState(item.vocabularyItemId, uiState.selectedLeftId, uiState.incorrectLeftId, uiState),
                                    onClick = { onLeftSelected(item.vocabularyItemId) },
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = Dimens.spacingSmall)) {
                            uiState.rightColumn.forEach { item ->
                                val state = tileState(item.vocabularyItemId, uiState.selectedRightId, uiState.incorrectRightId, uiState)
                                MatchTile(
                                    text = item.text,
                                    number = leftNumbers[item.vocabularyItemId]?.takeIf { state == MatchTileState.MATCHED },
                                    state = state,
                                    onClick = { onRightSelected(item.vocabularyItemId) },
                                )
                            }
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
    incorrectId: Long?,
    uiState: WordMatchUiState.Loaded,
): MatchTileState =
    when {
        uiState.matchedIds.contains(itemId) -> MatchTileState.MATCHED
        incorrectId == itemId -> MatchTileState.INCORRECT
        selectedId == itemId -> MatchTileState.SELECTED
        else -> MatchTileState.DEFAULT
    }

@Composable
private fun MatchTile(
    text: String,
    state: MatchTileState,
    onClick: () -> Unit,
    number: Int? = null,
) {
    val background = when (state) {
        MatchTileState.MATCHED -> LexiconSuccess
        MatchTileState.INCORRECT -> LexiconError
        MatchTileState.SELECTED -> MaterialTheme.colorScheme.primary
        MatchTileState.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
    }
    val enabled = state != MatchTileState.MATCHED
    val textColor = when (state) {
        MatchTileState.MATCHED, MatchTileState.INCORRECT -> Color.White
        MatchTileState.SELECTED -> MaterialTheme.colorScheme.onPrimary
        MatchTileState.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.tileVerticalSpacing)
            .background(background, RoundedCornerShape(Dimens.spacingSmall))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Dimens.spacingSmall),
    ) {
        val label = number?.let { "$it. $text" } ?: text
        Text(label, color = textColor)
    }
}

private val previewLeftColumn = listOf(
    WordMatchColumnItem(vocabularyItemId = 1, text = "praca"),
    WordMatchColumnItem(vocabularyItemId = 2, text = "dom"),
    WordMatchColumnItem(vocabularyItemId = 3, text = "kot"),
)
private val previewRightColumn = listOf(
    WordMatchColumnItem(vocabularyItemId = 2, text = "house"),
    WordMatchColumnItem(vocabularyItemId = 3, text = "cat"),
    WordMatchColumnItem(vocabularyItemId = 1, text = "work"),
)

@Preview(showBackground = true)
@Composable
private fun WordMatchScreenPreview() {
    LexiconTheme {
        WordMatchScreenContent(
            uiState =
                WordMatchUiState.Loaded(
                    stepIndex = 1,
                    totalSteps = 5,
                    leftColumn = previewLeftColumn,
                    rightColumn = previewRightColumn,
                    matchedIds = setOf(1),
                    selectedLeftId = 2,
                ),
            onClose = {},
            onLeftSelected = {},
            onRightSelected = {},
        )
    }
}
