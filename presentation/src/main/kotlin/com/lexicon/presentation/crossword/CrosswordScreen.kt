package com.lexicon.presentation.crossword

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexicon.interactors.crossword.CrosswordDirection
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import org.koin.androidx.compose.koinViewModel

private val MinCellSize = 14.dp
private val MaxCellSize = 40.dp

private const val LETTER_SIZE_RATIO = 0.5f

private const val GRID_HEIGHT_FRACTION = 0.55f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrosswordScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrosswordViewModel = koinViewModel(),
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
        onCellTapped = viewModel::onCellTapped,
        onLetterEntered = viewModel::onLetterEntered,
        onTipRequested = viewModel::onTipRequested,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrosswordScreenContent(
    uiState: CrosswordUiState,
    onClose: () -> Unit,
    onClueSelected: (Long) -> Unit,
    onCellTapped: (CrosswordCell) -> Unit,
    onLetterEntered: (CrosswordCell, String) -> Unit,
    onTipRequested: () -> Unit,
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
                    CrosswordGrid(
                        uiState = uiState,
                        onCellTapped = onCellTapped,
                        onLetterEntered = onLetterEntered,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
                    )

                    if (uiState.submitFailed) {
                        Text(
                            text = stringResource(R.string.crossword_check_failed),
                            color = LexiconError,
                            modifier = Modifier.padding(horizontal = Dimens.spacingMedium),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    ClueList(
                        uiState = uiState,
                        onClueSelected = onClueSelected,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimens.spacingMedium),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall, Alignment.End),
                    ) {
                        TextButton(onClick = debounced(onClick = onTipRequested), enabled = uiState.canUseTip) {
                            Text(stringResource(R.string.action_tip))
                        }
                    }
                }
        }
    }
}

@Composable
private fun CrosswordGrid(
    uiState: CrosswordUiState.Loaded,
    onCellTapped: (CrosswordCell) -> Unit,
    onLetterEntered: (CrosswordCell, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCells = uiState.selectedWord?.occupiedCells()?.toSet().orEmpty()
    val focusRequesters = remember(uiState.cells.keys) {
        uiState.cells.keys.associateWith { FocusRequester() }
    }

    LaunchedEffect(uiState.focusedCell) {
        uiState.focusedCell?.let { cell -> runCatching { focusRequesters[cell]?.requestFocus() } }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPerCell = maxWidth / uiState.colCount.coerceAtLeast(1)
        val heightPerCell = (maxHeight * GRID_HEIGHT_FRACTION) / uiState.rowCount.coerceAtLeast(1)
        val cellSize = minOf(widthPerCell, heightPerCell).coerceIn(MinCellSize, MaxCellSize)

        Column {
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
                                focusRequester = focusRequesters.getValue(cell),
                                onTapped = onCellTapped,
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
    focusRequester: FocusRequester,
    onTapped: (CrosswordCell) -> Unit,
    onLetterEntered: (CrosswordCell, String) -> Unit,
) {
    val background = when {
        state.locked -> MaterialTheme.colorScheme.secondaryContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = MaterialTheme.colorScheme.onSurface

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
                fontSize = (size.value * LETTER_SIZE_RATIO).sp,
                lineHeight = (size.value * LETTER_SIZE_RATIO).sp,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            cursorBrush = SolidColor(textColor),
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight()
                .focusRequester(focusRequester),
        )

        if (state.isFilled && enabled && !state.locked) {
            Box(modifier = Modifier.fillMaxSize().clickable { onTapped(cell) })
        }
    }
}

@Composable
private fun ClueList(
    uiState: CrosswordUiState.Loaded,
    onClueSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        uiState.words.forEach { word ->
            val isSelected = word.vocabularyItemId == uiState.selectedWordId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spacingTiny)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        LexiconShapes.small,
                    )
                    .clickable(enabled = uiState.isEditable) { onClueSelected(word.vocabularyItemId) }
                    .padding(Dimens.spacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = word.clueText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = if (word.direction == CrosswordDirection.ACROSS) {
                        stringResource(R.string.crossword_across_format, word.length)
                    } else {
                        stringResource(R.string.crossword_down_format, word.length)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        clueText = "cat",
        expectedText = "kot",
    ),
    CrosswordWordUi(
        vocabularyItemId = 2,
        row = 0,
        col = 2,
        direction = CrosswordDirection.DOWN,
        length = 3,
        clueText = "track",
        expectedText = "tor",
    ),
)

@LightDarkPreview
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
            onCellTapped = {},
            onLetterEntered = { _, _ -> },
            onTipRequested = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CrosswordScreenPartiallyFilledPreview() {
    LexiconTheme {
        CrosswordScreenContent(
            uiState =
                CrosswordUiState.Loaded(
                    words = previewWords.mapIndexed { index, word ->
                        if (index == 0) word.copy(revealedLetterCount = 1) else word
                    },
                    cells = previewWords.flatMap { it.occupiedCells() }.associateWith { CrosswordCellState() } +
                        mapOf(
                            CrosswordCell(0, 0) to CrosswordCellState(letter = "K", locked = true),
                            CrosswordCell(0, 1) to CrosswordCellState(letter = "O"),
                        ),
                    rowCount = 3,
                    colCount = 3,
                    selectedWordId = 1,
                ),
            onClose = {},
            onClueSelected = {},
            onCellTapped = {},
            onLetterEntered = { _, _ -> },
            onTipRequested = {},
        )
    }
}
