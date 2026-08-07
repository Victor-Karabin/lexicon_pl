package com.lexicon.presentation.crossword

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.lexicon.interactors.crossword.CrosswordDirection
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme

/** Cells shrink to fit the widest grid on screen; below this they stop being tappable/readable. */
private val MinCellSize = 22.dp
private val MaxCellSize = 40.dp
private val ClueImageSize = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrosswordScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrosswordViewModel = hiltViewModel(),
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

    CrosswordScreenContent(
        uiState = uiState,
        onClose = onClose,
        onClueSelected = viewModel::onClueSelected,
        onCellSelected = viewModel::onCellSelected,
        onLetterEntered = viewModel::onLetterEntered,
        onTipRequested = viewModel::onTipRequested,
        onCheck = viewModel::onCheck,
        onContinue = viewModel::onContinue,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrosswordScreenContent(
    uiState: CrosswordUiState,
    onClose: () -> Unit,
    onClueSelected: (Long) -> Unit,
    onCellSelected: (CrosswordCell) -> Unit,
    onLetterEntered: (CrosswordCell, String) -> Unit,
    onTipRequested: () -> Unit,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.crossword_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is CrosswordUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is CrosswordUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.spacingMedium),
                    ) {
                        CrosswordGrid(
                            uiState = uiState,
                            onCellSelected = onCellSelected,
                            onLetterEntered = onLetterEntered,
                        )

                        ClueRow(
                            uiState = uiState,
                            onClueSelected = onClueSelected,
                            modifier = Modifier.padding(top = Dimens.spacingMedium),
                        )

                        val statusLabel = when (uiState.answerState) {
                            is AnswerState.Correct -> stringResource(R.string.status_correct)
                            is AnswerState.Incorrect -> stringResource(R.string.status_incorrect)
                            else -> null
                        }
                        statusLabel?.let { label ->
                            val statusColor = if (uiState.answerState is AnswerState.Correct) LexiconSuccess else LexiconError
                            Text(
                                text = label,
                                color = statusColor,
                                modifier = Modifier.padding(top = Dimens.spacingMedium),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        if (uiState.submitFailed) {
                            Text(
                                text = stringResource(R.string.crossword_check_failed),
                                color = LexiconError,
                                modifier = Modifier.padding(top = Dimens.spacingMedium),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    // No Skip (single-screen training). Check turns into Next once validated, so the
                    // marked-up grid stays on screen until the user chooses to move on.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall, Alignment.End),
                    ) {
                        if (!uiState.isChecked) {
                            TextButton(onClick = debounced(onClick = onTipRequested), enabled = uiState.canUseTip) {
                                Text(stringResource(R.string.action_tip))
                            }
                        }
                        Button(
                            onClick = debounced { if (uiState.isChecked) onContinue() else onCheck() },
                            enabled = uiState.canCheck || uiState.awaitingContinue,
                        ) {
                            Text(stringResource(if (uiState.isChecked) R.string.action_next else R.string.action_check))
                        }
                    }
                }
        }
    }
}

@Composable
private fun CrosswordGrid(
    uiState: CrosswordUiState.Loaded,
    onCellSelected: (CrosswordCell) -> Unit,
    onLetterEntered: (CrosswordCell, String) -> Unit,
) {
    val selectedCells = uiState.selectedWord?.occupiedCells()?.toSet().orEmpty()
    BoxWithConstraints {
        // Generated grids routinely run 10+ columns wide, which overflows a phone at a fixed cell
        // size. Scale cells to the available width so the whole puzzle stays visible at once;
        // only fall back to scrolling for the rare grid too wide even at the minimum size.
        val cellSize = (maxWidth / uiState.colCount.coerceAtLeast(1))
            .coerceIn(MinCellSize, MaxCellSize)
        val scrollModifier = if (cellSize * uiState.colCount > maxWidth) {
            Modifier.horizontalScroll(rememberScrollState())
        } else {
            Modifier
        }

        Column(modifier = scrollModifier) {
            for (row in 0 until uiState.rowCount) {
                Row {
                    for (col in 0 until uiState.colCount) {
                        val cell = CrosswordCell(row, col)
                        val cellState = uiState.cells[cell]
                        if (cellState == null) {
                            Box(modifier = Modifier.size(cellSize))
                        } else {
                            CrosswordCellBox(
                                cell = cell,
                                state = cellState,
                                size = cellSize,
                                isSelected = cell in selectedCells,
                                enabled = uiState.isEditable,
                                onSelected = onCellSelected,
                                onLetterEntered = onLetterEntered,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrosswordCellBox(
    cell: CrosswordCell,
    state: CrosswordCellState,
    size: Dp,
    isSelected: Boolean,
    enabled: Boolean,
    onSelected: (CrosswordCell) -> Unit,
    onLetterEntered: (CrosswordCell, String) -> Unit,
) {
    val background = when {
        state.isIncorrect -> LexiconError
        state.locked -> MaterialTheme.colorScheme.secondaryContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = if (state.isIncorrect) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .background(background, LexiconShapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outline, LexiconShapes.small),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = state.letter,
            onValueChange = { onLetterEntered(cell, it) },
            enabled = enabled && !state.locked,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            cursorBrush = SolidColor(textColor),
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight()
                .onFocusChanged { if (it.isFocused) onSelected(cell) },
        )
    }
}

@Composable
private fun ClueRow(
    uiState: CrosswordUiState.Loaded,
    onClueSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        items(uiState.words.size) { index ->
            val word = uiState.words[index]
            val isSelected = word.vocabularyItemId == uiState.selectedWordId
            Column(
                modifier = Modifier
                    .width(ClueImageSize)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        LexiconShapes.small,
                    )
                    .clickable(enabled = uiState.isEditable) { onClueSelected(word.vocabularyItemId) }
                    .padding(Dimens.spacingSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(ClueImageSize), contentAlignment = Alignment.Center) {
                    if (word.imageUrl == null) {
                        Text(word.clueText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    } else {
                        SubcomposeAsyncImage(
                            model = word.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            },
                            error = {
                                Text(word.clueText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            },
                        )
                    }
                }
                Text(
                    text = if (word.direction == CrosswordDirection.ACROSS) {
                        stringResource(R.string.crossword_across_format, word.length)
                    } else {
                        stringResource(R.string.crossword_down_format, word.length)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = Dimens.spacingTiny),
                )
                // Spec §14: the correct answers are revealed once the crossword has been checked.
                uiState.revealedAnswer(word)?.let { answer ->
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Dimens.spacingTiny),
                    )
                }
            }
        }
    }
}

private val previewWords = listOf(
    CrosswordWordUi(
        vocabularyItemId = 1,
        row = 0,
        col = 0,
        direction = CrosswordDirection.ACROSS,
        length = 3,
        imageUrl = null,
        clueText = "cat",
        expectedText = "kot",
    ),
    CrosswordWordUi(
        vocabularyItemId = 2,
        row = 0,
        col = 2,
        direction = CrosswordDirection.DOWN,
        length = 3,
        imageUrl = null,
        clueText = "track",
        expectedText = "tor",
    ),
)

@Preview(showBackground = true)
@Composable
private fun CrosswordScreenPreview() {
    LexiconTheme {
        CrosswordScreenContent(
            uiState =
                CrosswordUiState.Loaded(
                    words = previewWords,
                    cells = previewWords.flatMap { it.occupiedCells() }.associateWith { CrosswordCellState() },
                    rowCount = 3,
                    colCount = 3,
                    selectedWordId = 1,
                ),
            onClose = {},
            onClueSelected = {},
            onCellSelected = {},
            onLetterEntered = { _, _ -> },
            onTipRequested = {},
            onCheck = {},
            onContinue = {},
        )
    }
}
