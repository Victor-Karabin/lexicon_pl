package com.lexicon.presentation.passage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.presentation.R
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.course.ExerciseAudioButton
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.AnswerChip
import com.lexicon.presentation.theme.component.AnswerChipState
import org.koin.androidx.compose.koinViewModel

@Composable
fun PassageScreen(
    withWordBank: Boolean,
    onSessionComplete: (Int, Int, Int, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PassageViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    PassageContent(
        uiState = uiState,
        withWordBank = withWordBank,
        onClose = onClose,
        onSpeak = viewModel::onSpeak,
        onAnswerChanged = viewModel::onAnswerChanged,
        onBankWordSelected = viewModel::onBankWordSelected,
        onGapCleared = viewModel::onGapCleared,
        onCheck = { viewModel.onCheck(onSessionComplete) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassageContent(
    uiState: PassageUiState,
    withWordBank: Boolean,
    onClose: () -> Unit,
    onSpeak: () -> Unit,
    onAnswerChanged: (Int, String) -> Unit,
    onBankWordSelected: (String) -> Unit,
    onGapCleared: (Int) -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrainingTopBar(
                title = stringResource(
                    if (withWordBank) R.string.passage_bank_title else R.string.passage_write_title,
                ),
                onClose = onClose,
            )
        },
    ) { padding ->
        val passage = uiState.passage
        when {
            uiState.isLoading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            passage == null ->
                Box(
                    Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (uiState.problem) {
                                PassageProblem.OFFLINE -> R.string.passage_offline
                                PassageProblem.REFUSED -> R.string.passage_refused
                                else -> R.string.passage_none
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
                        Text(
                            text = passage.level,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        ExerciseAudioButton(isPlaying = uiState.isSpeaking, onClick = onSpeak)

                        PassageBody(
                            uiState = uiState,
                            withWordBank = withWordBank,
                            onAnswerChanged = onAnswerChanged,
                            onGapCleared = onGapCleared,
                        )
                    }

                    if (withWordBank) {
                        HorizontalDivider()
                        WordBank(
                            uiState = uiState,
                            onBankWordSelected = onBankWordSelected,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall, Alignment.End),
                    ) {
                        if (uiState.isChecked) {
                            Text(
                                text = stringResource(
                                    R.string.exercise_score,
                                    uiState.correctCount,
                                    uiState.expected.size,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        } else {
                            Button(onClick = onCheck) { Text(stringResource(R.string.exercise_check)) }
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassageBody(
    uiState: PassageUiState,
    withWordBank: Boolean,
    onAnswerChanged: (Int, String) -> Unit,
    onGapCleared: (Int) -> Unit,
) {
    val passage = uiState.passage ?: return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
        verticalArrangement = Arrangement.Center,
    ) {
        var gap = 0
        passage.segments.forEach { segment ->
            when (segment) {
                is PassageSegment.Text ->
                    segment.text.split(" ").filter { it.isNotBlank() }.forEach { word ->
                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = Dimens.spacingSmall),
                        )
                    }

                is PassageSegment.Gap -> {
                    val at = gap++
                    PassageGap(
                        value = uiState.answers.getOrElse(at) { "" },
                        expected = segment.answer,
                        isCorrect = uiState.correctness.getOrNull(at),
                        isChecked = uiState.isChecked,
                        readOnly = withWordBank,
                        onValueChanged = { onAnswerChanged(at, it) },
                        onCleared = { onGapCleared(at) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordBank(
    uiState: PassageUiState,
    onBankWordSelected: (String) -> Unit,
) {
    val used = uiState.usedBankWords
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(Dimens.spacingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        uiState.bank.forEach { word ->
            AnswerChip(
                label = word,
                state = if (word in used) AnswerChipState.SELECTED else AnswerChipState.UNSELECTED,
                onClick = { onBankWordSelected(word) }.takeIf { !uiState.isChecked && word !in used },
            )
        }
    }
}
