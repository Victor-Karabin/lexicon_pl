package com.lexicon.presentation.fillword

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.presentation.R
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconSuccessContainer
import com.lexicon.presentation.theme.component.GridCell
import com.lexicon.presentation.theme.component.gridLetterStyle
import org.koin.androidx.compose.koinViewModel

private val MinCellSize = 20.dp
private val MaxCellSize = 44.dp

@Composable
fun FillwordScreen(
    onSessionComplete: (Int, Int, Int, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FillwordViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    FillwordContent(
        uiState = uiState,
        onClose = onClose,
        onCellTapped = viewModel::onCellTapped,
        onDone = { onSessionComplete(uiState.found.size, uiState.total - uiState.found.size, 0, 0) },
        modifier = modifier,
    )
}

@Composable
private fun FillwordContent(
    uiState: FillwordUiState,
    onClose: () -> Unit,
    onCellTapped: (FillwordCell) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.fillword_title), onClose = onClose) },
    ) { padding ->
        val puzzle = uiState.puzzle
        when {
            uiState.isLoading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            puzzle == null ->
                Box(
                    Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (uiState.problem) {
                                FillwordProblem.OFFLINE -> R.string.fillword_offline
                                FillwordProblem.REFUSED -> R.string.fillword_refused
                                else -> R.string.fillword_none
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            else ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.spacingMedium),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                    ) {
                        Grid(uiState = uiState, onCellTapped = onCellTapped)
                        WordsToFind(uiState = uiState)
                    }

                    TrainingActionRow(
                        onCheck = onDone,
                        onNext = onDone,
                        awaitingNext = uiState.isComplete,
                        checkEnabled = true,
                    )
                }
        }
    }
}

@Composable
private fun Grid(
    uiState: FillwordUiState,
    onCellTapped: (FillwordCell) -> Unit,
) {
    val puzzle = uiState.puzzle ?: return
    val found = uiState.foundCells

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth / puzzle.size.coerceAtLeast(1)).coerceIn(MinCellSize, MaxCellSize)

        Column {
            for (row in 0 until puzzle.size) {
                Row {
                    for (column in 0 until puzzle.size) {
                        val cell = FillwordCell(row, column)
                        val isFound = cell in found
                        val isAnchor = uiState.anchor == cell

                        GridCell(
                            size = cellSize,
                            background = when {
                                isFound -> LexiconSuccessContainer
                                isAnchor -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = when {
                                isFound -> LexiconSuccess
                                isAnchor -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.clickable { onCellTapped(cell) },
                        ) {
                            Text(
                                text = puzzle.letterAt(cell),
                                style = gridLetterStyle(
                                    size = cellSize,
                                    color = if (isFound) LexiconSuccess else MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordsToFind(uiState: FillwordUiState) {
    val puzzle = uiState.puzzle ?: return

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        Text(
            text = stringResource(R.string.fillword_found, uiState.found.size, uiState.total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            puzzle.words.forEach { word ->
                val isFound = word.word in uiState.found
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFound) LexiconSuccess else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isFound) TextDecoration.LineThrough else null,
                )
            }
        }
    }
}
