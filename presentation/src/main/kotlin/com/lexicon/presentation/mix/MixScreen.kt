package com.lexicon.presentation.mix

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.mix.MixStep
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerOptionList
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.AnswerStatusLabel
import com.lexicon.presentation.common.BuiltAnswerField
import com.lexicon.presentation.common.ClueImage
import com.lexicon.presentation.common.LetterTile
import com.lexicon.presentation.common.LetterTileGrid
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.common.shuffleIntoTiles
import com.lexicon.presentation.pronunciation.RecordingState
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.PlayButton
import com.lexicon.presentation.theme.component.ProgressDots
import com.lexicon.presentation.theme.component.WordCard

private val TrueFalseButtonHeight = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MixViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasRecordAudioPermission = granted
            if (granted) viewModel.onRecordRequested()
        }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    MixScreenContent(
        uiState = uiState,
        onClose = onClose,
        onAnswerChanged = viewModel::onAnswerChanged,
        onTileSelected = viewModel::onTileSelected,
        onOptionSelected = viewModel::onOptionSelected,
        onReplayAudio = viewModel::onReplayAudio,
        onRecordRequested = {
            if (hasRecordAudioPermission) {
                viewModel.onRecordRequested()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onTrueOrFalseAnswer = viewModel::onTrueOrFalseAnswer,
        onUndo = viewModel::onUndo,
        onTipRequested = viewModel::onTipRequested,
        onSkip = viewModel::onSkip,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MixScreenContent(
    uiState: MixUiState,
    onClose: () -> Unit,
    onAnswerChanged: (String) -> Unit,
    onTileSelected: (LetterTile) -> Unit,
    onOptionSelected: (String) -> Unit,
    onReplayAudio: () -> Unit,
    onRecordRequested: () -> Unit,
    onTrueOrFalseAnswer: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onTipRequested: () -> Unit,
    onSkip: () -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.mix_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is MixUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is MixUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(Dimens.spacingMedium),
                    ) {
                        ProgressDots(
                            step = uiState.stepIndex,
                            total = uiState.totalSteps,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Names the exercise, so a step change doesn't look like a glitch.
                        Text(
                            text = uiState.trainingType.label(),
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        StepContent(
                            uiState = uiState,
                            onAnswerChanged = onAnswerChanged,
                            onTileSelected = onTileSelected,
                            onOptionSelected = onOptionSelected,
                            onReplayAudio = onReplayAudio,
                            onRecordRequested = onRecordRequested,
                            onTrueOrFalseAnswer = onTrueOrFalseAnswer,
                        )

                        StepFeedback(uiState)
                    }

                    // True or False answers on tap, so before it is answered there is no primary
                    // action to offer; showing a Check it can never enable reads as a broken step.
                    if (uiState.hasCheckAction || uiState.awaitingNext) {
                        TrainingActionRow(
                            onCheck = onCheck,
                            onNext = onNext,
                            awaitingNext = uiState.awaitingNext,
                            checkEnabled = uiState.canCheck,
                            onUndo = onUndo.takeIf { uiState.canUndo },
                            onTip = onTipRequested.takeIf { uiState.canUseTip },
                            onSkip = onSkip.takeIf { uiState.canSkip },
                        )
                    }
                }
        }
    }
}

@Composable
private fun ColumnScope.StepContent(
    uiState: MixUiState.Loaded,
    onAnswerChanged: (String) -> Unit,
    onTileSelected: (LetterTile) -> Unit,
    onOptionSelected: (String) -> Unit,
    onReplayAudio: () -> Unit,
    onRecordRequested: () -> Unit,
    onTrueOrFalseAnswer: (Boolean) -> Unit,
) {
    when (val step = uiState.step) {
        is MixStep.Dictation -> {
            PlayButton(
                onClick = onReplayAudio,
                label = stringResource(R.string.action_listen_again),
                modifier = Modifier.padding(top = Dimens.spacingLarge),
            )
            OutlinedTextField(
                value = uiState.answerText,
                onValueChange = onAnswerChanged,
                readOnly = !uiState.isEditable,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
                label = { Text(stringResource(R.string.dictation_type_what_you_heard)) },
            )
        }

        is MixStep.DictationPuzzle -> {
            PlayButton(
                onClick = onReplayAudio,
                label = stringResource(R.string.action_listen_again),
                modifier = Modifier.padding(top = Dimens.spacingLarge),
            )
            BuiltAnswerField(
                answer = uiState.builtAnswer,
                answerState = uiState.answerState,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
            LetterTileGrid(
                tiles = uiState.availableTiles,
                onTileSelected = onTileSelected,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
        }

        is MixStep.Puzzle -> {
            ClueImage(
                imageUrl = step.step.imageUrl,
                fallbackText = step.step.clueText,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
            BuiltAnswerField(
                answer = uiState.builtAnswer,
                answerState = uiState.answerState,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
            LetterTileGrid(
                tiles = uiState.availableTiles,
                onTileSelected = onTileSelected,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
        }

        is MixStep.ImageTest -> {
            ClueImage(
                imageUrl = step.step.imageUrl,
                fallbackText = step.step.clueText,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
            AnswerOptionList(
                options = step.step.options,
                selectedOption = uiState.selectedOption,
                correctOption = uiState.correctOption,
                enabled = uiState.isEditable,
                onOptionSelected = onOptionSelected,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
        }

        is MixStep.TrueOrFalse -> {
            WordCard(
                word = step.step.word,
                sublabel = step.step.displayedTranslation,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(TrueFalseButtonHeight).padding(top = Dimens.spacingMedium),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                TrueOrFalseButton(
                    label = stringResource(R.string.action_true),
                    outcome = uiState.trueOrFalseOutcomeFor(isTrueButton = true),
                    enabled = uiState.isEditable,
                    onClick = { onTrueOrFalseAnswer(true) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
                TrueOrFalseButton(
                    label = stringResource(R.string.action_false),
                    outcome = uiState.trueOrFalseOutcomeFor(isTrueButton = false),
                    enabled = uiState.isEditable,
                    onClick = { onTrueOrFalseAnswer(false) },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }
        }

        is MixStep.Pronunciation -> {
            Text(
                text = step.step.clueText,
                modifier = Modifier.padding(top = Dimens.spacingLarge),
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(
                onClick = debounced(onClick = onRecordRequested),
                enabled = uiState.canRecord,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            ) {
                Text(
                    when (uiState.recordingState) {
                        RecordingState.IDLE, RecordingState.RECORDED -> stringResource(R.string.pronunciation_record)
                        RecordingState.RECORDING -> stringResource(R.string.pronunciation_listening)
                        RecordingState.PROCESSING -> stringResource(R.string.pronunciation_recognizing)
                    },
                )
            }
            if (uiState.answerText.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.pronunciation_heard_format, uiState.answerText),
                    modifier = Modifier.padding(top = Dimens.spacingMedium),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * [outcome]: true = correct, false = incorrect, null = default colour.
 *
 * The button is disabled once answered, so the outcome colour is repeated as the disabled colour —
 * otherwise Material greys the container out and the result indication disappears at the moment it
 * becomes relevant.
 */
@Composable
private fun TrueOrFalseButton(
    label: String,
    outcome: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = when (outcome) {
        true -> LexiconSuccess
        false -> LexiconError
        null -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when (outcome) {
        null -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> Color.White
    }
    Button(
        onClick = debounced(onClick = onClick),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
        modifier = modifier,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun StepFeedback(uiState: MixUiState.Loaded) {
    AnswerStatusLabel(
        answerState = uiState.answerState,
        modifier = Modifier.padding(top = Dimens.spacingMedium),
    )

    if (uiState.isEditable) {
        uiState.tipText?.let { hint ->
            Text(
                text = stringResource(R.string.hint_format, hint),
                modifier = Modifier.padding(top = Dimens.spacingSmall),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    uiState.revealedAnswer?.let { answer ->
        Text(
            text = stringResource(R.string.expected_format, answer),
            modifier = Modifier.padding(top = Dimens.spacingSmall),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun com.lexicon.interactors.mix.MixTrainingType.label(): String =
    stringResource(
        when (this) {
            com.lexicon.interactors.mix.MixTrainingType.DICTATION -> R.string.dictation_title
            com.lexicon.interactors.mix.MixTrainingType.DICTATION_PUZZLE -> R.string.dictation_puzzle_title
            com.lexicon.interactors.mix.MixTrainingType.PUZZLE -> R.string.puzzle_title
            com.lexicon.interactors.mix.MixTrainingType.IMAGE_TEST -> R.string.image_test_title
            com.lexicon.interactors.mix.MixTrainingType.TRUE_OR_FALSE -> R.string.true_or_false_title
            com.lexicon.interactors.mix.MixTrainingType.PRONUNCIATION_CHECK -> R.string.pronunciation_title
        },
    )

private val previewTiles = shuffleIntoTiles("praca")

@LightDarkPreview
@Composable
private fun MixScreenPuzzleStepPreview() {
    LexiconTheme {
        MixScreenContent(
            uiState = MixUiState.Loaded(
                stepIndex = 2,
                totalSteps = 10,
                step = MixStep.Puzzle(
                    2,
                    com.lexicon.interactors.puzzle.PuzzleStepResponse(2, 1L, "praca", null, "work"),
                ),
                stepTiles = previewTiles,
                placedTiles = previewTiles.take(2),
            ),
            onClose = {},
            onAnswerChanged = {},
            onTileSelected = {},
            onOptionSelected = {},
            onReplayAudio = {},
            onRecordRequested = {},
            onTrueOrFalseAnswer = {},
            onUndo = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

/** True or False inside Mix: one question, no countdown. */
@LightDarkPreview
@Composable
private fun MixScreenTrueOrFalseStepPreview() {
    LexiconTheme {
        MixScreenContent(
            uiState = MixUiState.Loaded(
                stepIndex = 4,
                totalSteps = 10,
                step = MixStep.TrueOrFalse(
                    4,
                    com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse(4, 2L, "chleb", "bread", true),
                ),
            ),
            onClose = {},
            onAnswerChanged = {},
            onTileSelected = {},
            onOptionSelected = {},
            onReplayAudio = {},
            onRecordRequested = {},
            onTrueOrFalseAnswer = {},
            onUndo = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

/** Answered incorrectly: only the tapped button takes the outcome colour. */
@LightDarkPreview
@Composable
private fun MixScreenTrueOrFalseAnsweredPreview() {
    LexiconTheme {
        MixScreenContent(
            uiState = MixUiState.Loaded(
                stepIndex = 4,
                totalSteps = 10,
                step = MixStep.TrueOrFalse(
                    4,
                    com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse(4, 2L, "chleb", "bread", true),
                ),
                answerState = AnswerState.Incorrect(),
                answeredTrue = false,
            ),
            onClose = {},
            onAnswerChanged = {},
            onTileSelected = {},
            onOptionSelected = {},
            onReplayAudio = {},
            onRecordRequested = {},
            onTrueOrFalseAnswer = {},
            onUndo = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
