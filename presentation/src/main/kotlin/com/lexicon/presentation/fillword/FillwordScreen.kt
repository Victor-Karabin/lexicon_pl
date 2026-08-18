package com.lexicon.presentation.fillword

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.presentation.R
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconErrorContainer
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
        onCellsTraced = viewModel::onCellsTraced,
        onCheck = viewModel::onCheck,
        onDone = {
            viewModel.onFinished()
            onSessionComplete(uiState.found.size, uiState.total - uiState.found.size, 0, 0)
        },
        modifier = modifier,
    )
}

@Composable
private fun FillwordContent(
    uiState: FillwordUiState,
    onClose: () -> Unit,
    onCellTapped: (FillwordCell) -> Unit,
    onCellsTraced: (FillwordCell, FillwordCell) -> Unit,
    onCheck: () -> Unit,
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
                        text = stringResource(R.string.fillword_none),
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
                        Grid(
                            uiState = uiState,
                            onCellTapped = onCellTapped,
                            onCellsTraced = onCellsTraced,
                        )
                        WordsToFind(uiState = uiState)
                    }

                    // Check gives away whatever is still hidden rather than marking
                    // anything; once there is nothing left to give away, only Done remains.
                    TrainingActionRow(
                        onCheck = onCheck,
                        onNext = onDone,
                        awaitingNext = uiState.isFinished,
                        checkEnabled = true,
                        nextLabel = R.string.action_done,
                    )
                }
        }
    }
}

@Composable
private fun Grid(
    uiState: FillwordUiState,
    onCellTapped: (FillwordCell) -> Unit,
    onCellsTraced: (FillwordCell, FillwordCell) -> Unit,
) {
    val puzzle = uiState.puzzle ?: return

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth / puzzle.size.coerceAtLeast(1)).coerceIn(MinCellSize, MaxCellSize)
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }

        var from by remember(puzzle) { mutableStateOf<FillwordCell?>(null) }
        var to by remember(puzzle) { mutableStateOf<FillwordCell?>(null) }

        val cellAt: (Offset) -> FillwordCell? = { offset ->
            val row = (offset.y / cellPx).toInt()
            val column = (offset.x / cellPx).toInt()
            FillwordCell(row, column)
                .takeIf { offset.x >= 0f && offset.y >= 0f && row < puzzle.size && column < puzzle.size }
        }

        val tracing = puzzle.tracedCells(from, to)

        Box(
            modifier = Modifier
                .pointerInput(puzzle, cellPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            from = cellAt(offset)
                            to = from
                        },
                        onDrag = { change, _ -> cellAt(change.position)?.let { to = it } },
                        onDragEnd = {
                            val start = from
                            val finish = to
                            if (start != null && finish != null && start != finish) {
                                onCellsTraced(start, finish)
                            }
                            from = null
                            to = null
                        },
                        onDragCancel = {
                            from = null
                            to = null
                        },
                    )
                }.pointerInput(puzzle, cellPx) {
                    detectTapGestures { offset -> cellAt(offset)?.let(onCellTapped) }
                },
        ) {
            Column {
                for (row in 0 until puzzle.size) {
                    Row {
                        for (column in 0 until puzzle.size) {
                            val cell = FillwordCell(row, column)
                            val isFound = cell in uiState.foundCells
                            val isMissed = cell in uiState.missingCells
                            val isLive = cell in tracing || uiState.anchor == cell

                            GridCell(
                                size = cellSize,
                                background = when {
                                    isFound -> LexiconSuccessContainer
                                    isMissed -> LexiconErrorContainer
                                    isLive -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                border = when {
                                    isFound -> LexiconSuccess
                                    isMissed -> LexiconError
                                    isLive -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                            ) {
                                Text(
                                    text = puzzle.letterAt(cell),
                                    style = gridLetterStyle(
                                        size = cellSize,
                                        color = when {
                                            isFound -> LexiconSuccess
                                            isMissed -> LexiconError
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The run under the finger right now, so the learner can see what they are about to claim. */
private fun FillwordPuzzle.tracedCells(
    from: FillwordCell?,
    to: FillwordCell?,
): Set<FillwordCell> {
    if (from == null) return emptySet()
    return runBetween(from, to ?: from).toSet()
}

@Composable
private fun WordsToFind(uiState: FillwordUiState) {
    val puzzle = uiState.puzzle ?: return
    val pending = MaterialTheme.colorScheme.onSurface
    val separator = MaterialTheme.colorScheme.onSurfaceVariant

    // One run of text rather than a row of separate labels: the clues then wrap the way a
    // sentence does, and a comma can never end up stranded at the start of a line.
    val clues = buildAnnotatedString {
        puzzle.words.forEachIndexed { index, word ->
            val isFound = word.word in uiState.found
            val isMissed = !isFound && uiState.isRevealed

            // Asked in English and answered in Polish: the clue is the meaning, and the
            // word itself appears once it is found — or once it is given away.
            withStyle(
                SpanStyle(
                    color = when {
                        isFound -> LexiconSuccess
                        isMissed -> LexiconError
                        else -> pending
                    },
                    textDecoration = if (isFound) TextDecoration.LineThrough else null,
                ),
            ) {
                append(if (isFound || isMissed) word.word else puzzle.translationOf(word))
            }

            if (index < puzzle.words.lastIndex) {
                withStyle(SpanStyle(color = separator)) { append(", ") }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        Text(
            text = stringResource(R.string.fillword_found, uiState.found.size, uiState.total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = clues, style = MaterialTheme.typography.bodyLarge)
    }
}
