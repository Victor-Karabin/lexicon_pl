package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.course.LessonExercise
import com.lexicon.interactors.course.questionCount
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.AnswerStatusLabel
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens

@Composable
fun ExerciseScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ExerciseContent(
        uiState = uiState,
        onClose = onClose,
        onPlayAudio = viewModel::onPlayAudio,
        onOptionSelected = viewModel::onOptionSelected,
        onGapChanged = viewModel::onGapChanged,
        onCheck = viewModel::onCheck,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseContent(
    uiState: ExerciseUiState,
    onClose: () -> Unit,
    onPlayAudio: () -> Unit,
    onOptionSelected: (Int, String) -> Unit,
    onGapChanged: (Int, Int, String) -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.exercises_heading), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is ExerciseUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is ExerciseUiState.NotFound ->
                Box(
                    Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.lesson_not_found), style = MaterialTheme.typography.bodyMedium)
                }

            is ExerciseUiState.Loaded ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(Dimens.spacingMedium),
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = uiState.exercise.instruction,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (uiState.exercise.audioFile != null) {
                                ExerciseAudioButton(
                                    isPlaying = uiState.isPlaying,
                                    onClick = onPlayAudio,
                                    modifier = Modifier.padding(top = Dimens.spacingMedium),
                                )
                            }
                            if (uiState.isAudioMissing) {
                                Text(
                                    text = stringResource(R.string.exercise_audio_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                                )
                            }
                        }
                    }

                    questions(uiState, onOptionSelected, onGapChanged)

                    if (uiState.exercise !is LessonExercise.Repeat) {
                        item { Footer(uiState, onCheck, onRetry) }
                    }
                }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.questions(
    uiState: ExerciseUiState.Loaded,
    onOptionSelected: (Int, String) -> Unit,
    onGapChanged: (Int, Int, String) -> Unit,
) {
    when (val exercise = uiState.exercise) {
        is LessonExercise.Repeat -> {
            item {
                Text(
                    text = stringResource(R.string.exercise_repeat_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.spacingMedium),
                )
            }
            itemsIndexed(exercise.words) { _, word ->
                Text(
                    text = word,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = Dimens.spacingSmall),
                )
            }
        }

        is LessonExercise.MinimalPair ->
            itemsIndexed(exercise.items) { index, item ->
                MinimalPairRow(
                    item = item,
                    selected = uiState.responses.getOrNull(index)?.firstOrNull(),
                    answerState = uiState.answerState,
                    onSelect = { onOptionSelected(index, it) },
                )
            }

        is LessonExercise.GapFill ->
            itemsIndexed(exercise.items) { index, item ->
                GapFillRow(
                    item = item,
                    values = uiState.responses.getOrNull(index).orEmpty(),
                    answerState = uiState.answerState,
                    onValueChanged = { gap, value -> onGapChanged(index, gap, value) },
                )
            }
    }
}

@Composable
private fun Footer(
    uiState: ExerciseUiState.Loaded,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge)) {
        if (uiState.answerState !is AnswerState.Unanswered) {
            AnswerStatusLabel(answerState = uiState.answerState)
            Text(
                text = stringResource(
                    R.string.exercise_score,
                    uiState.correctCount,
                    uiState.exercise.questionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            if (uiState.answerState is AnswerState.Unanswered) {
                Button(onClick = onCheck) { Text(stringResource(R.string.exercise_check)) }
            } else {
                OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.exercise_try_again)) }
            }
        }
    }
}
