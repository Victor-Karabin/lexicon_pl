package com.lexicon.presentation.pronunciation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.PlayButton
import com.lexicon.presentation.theme.component.ProgressDots

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciationScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PronunciationViewModel = hiltViewModel(),
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

    PronunciationScreenContent(
        uiState = uiState,
        onClose = onClose,
        onReplayReferenceAudio = viewModel::onReplayReferenceAudio,
        onRecordRequested = {
            if (hasRecordAudioPermission) {
                viewModel.onRecordRequested()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onTipRequested = viewModel::onTipRequested,
        onSkip = viewModel::onSkip,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PronunciationScreenContent(
    uiState: PronunciationUiState,
    onClose: () -> Unit,
    onReplayReferenceAudio: () -> Unit,
    onRecordRequested: () -> Unit,
    onTipRequested: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.pronunciation_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is PronunciationUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is PronunciationUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(Dimens.spacingMedium),
                    ) {
                        ProgressDots(
                            step = uiState.stepIndex,
                            total = uiState.totalSteps,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        PlayButton(
                            onClick = onReplayReferenceAudio,
                            label = stringResource(R.string.action_listen_again),
                            modifier = Modifier.padding(top = Dimens.spacingLarge),
                        )

                        Button(
                            onClick = debounced(onClick = onRecordRequested),
                            enabled = uiState.canRecord,
                            modifier = Modifier.padding(top = Dimens.spacingMedium),
                        ) {
                            Text(
                                when (uiState.recordingState) {
                                    RecordingState.IDLE -> stringResource(R.string.pronunciation_record)
                                    RecordingState.RECORDING -> stringResource(R.string.pronunciation_listening)
                                    RecordingState.PROCESSING -> stringResource(R.string.pronunciation_checking)
                                },
                            )
                        }

                        uiState.recognizedText?.let { recognized ->
                            Text(
                                text = stringResource(R.string.pronunciation_heard_format, recognized),
                                modifier = Modifier.padding(top = Dimens.spacingMedium),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        if (uiState.isEditable) {
                            uiState.tipTranslation?.let { hint ->
                                Text(
                                    text = stringResource(R.string.hint_format, hint),
                                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        val statusLabel = when (uiState.answerState) {
                            is AnswerState.Correct -> stringResource(R.string.status_correct)
                            is AnswerState.Incorrect -> stringResource(R.string.status_incorrect)
                            is AnswerState.Skipped -> stringResource(R.string.status_skipped)
                            is AnswerState.Unanswered -> null
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

                        uiState.revealedAnswer?.let { answer ->
                            Text(
                                text = stringResource(R.string.expected_format, answer),
                                modifier = Modifier.padding(top = Dimens.spacingSmall),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    // Recording auto-submits (there's no manual Check step), so this row only ever
                    // carries Tip/Skip plus a Next button once a step needs an explicit continue.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall, Alignment.End),
                    ) {
                        TextButton(onClick = debounced(onClick = onTipRequested), enabled = uiState.canUseTip) {
                            Text(stringResource(R.string.action_tip))
                        }
                        TextButton(onClick = debounced(onClick = onSkip), enabled = uiState.canSkip) {
                            Text(stringResource(R.string.action_skip))
                        }
                        if (uiState.awaitingNext) {
                            Button(onClick = debounced(onClick = onNext)) {
                                Text(stringResource(R.string.action_next))
                            }
                        }
                    }
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PronunciationScreenUnansweredPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    recordingState = RecordingState.IDLE,
                ),
            onClose = {},
            onReplayReferenceAudio = {},
            onRecordRequested = {},
            onTipRequested = {},
            onSkip = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PronunciationScreenCorrectPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    recordingState = RecordingState.IDLE,
                    recognizedText = "praca",
                    answerState = AnswerState.Correct,
                ),
            onClose = {},
            onReplayReferenceAudio = {},
            onRecordRequested = {},
            onTipRequested = {},
            onSkip = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PronunciationScreenIncorrectPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    recordingState = RecordingState.IDLE,
                    recognizedText = "prace",
                    answerState = AnswerState.Incorrect("praca"),
                ),
            onClose = {},
            onReplayReferenceAudio = {},
            onRecordRequested = {},
            onTipRequested = {},
            onSkip = {},
            onNext = {},
        )
    }
}
