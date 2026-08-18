package com.lexicon.presentation.pronunciation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.debounced
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.PlayButton
import com.lexicon.presentation.theme.component.ProgressDots
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciationScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    readsSentences: Boolean = false,
    viewModel: PronunciationViewModel = koinViewModel(),
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
        readsSentences = readsSentences,
        onClose = onClose,
        onRecordRequested = {
            if (hasRecordAudioPermission) {
                viewModel.onRecordRequested()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onPlayRecording = viewModel::onPlayRecording,
        onTipRequested = viewModel::onTipRequested,
        onSkip = viewModel::onSkip,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PronunciationScreenContent(
    uiState: PronunciationUiState,
    readsSentences: Boolean,
    onClose: () -> Unit,
    onRecordRequested: () -> Unit,
    onPlayRecording: () -> Unit,
    onTipRequested: () -> Unit,
    onSkip: () -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrainingTopBar(
                title = stringResource(
                    if (readsSentences) R.string.pronunciation_sentences_title else R.string.pronunciation_title,
                ),
                onClose = onClose,
            )
        },
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
            is PronunciationUiState.Unavailable ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (uiState.reason) {
                                UnavailableReason.OFFLINE -> R.string.pronunciation_sentences_offline
                                UnavailableReason.REFUSED -> R.string.pronunciation_sentences_refused
                                UnavailableReason.NO_FAVOURITES -> R.string.pronunciation_sentences_none
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            is PronunciationUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.spacingMedium),
                    ) {
                        ProgressDots(
                            step = uiState.stepIndex,
                            total = uiState.totalSteps,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = uiState.word,
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

                        uiState.recognitionError?.let { errorType ->
                            Text(
                                text = when (errorType) {
                                    RecognitionErrorType.UNAVAILABLE -> stringResource(R.string.pronunciation_recognition_unavailable)
                                    RecognitionErrorType.FAILED -> stringResource(R.string.pronunciation_recognition_failed)
                                },
                                color = LexiconError,
                                modifier = Modifier.padding(top = Dimens.spacingMedium),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        uiState.recognizedText?.let { recognized ->
                            Text(
                                text = stringResource(R.string.pronunciation_heard_format, recognized),
                                modifier = Modifier.padding(top = Dimens.spacingMedium),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        if (uiState.canPlayRecording) {
                            PlayButton(
                                onClick = onPlayRecording,
                                label = stringResource(R.string.pronunciation_play_recording),
                                modifier = Modifier.padding(top = Dimens.spacingSmall),
                            )
                        }

                        if (uiState.isEditable) {
                            uiState.tipTranslation?.let { translation ->
                                Text(
                                    text = stringResource(R.string.hint_format, translation),
                                    modifier = Modifier.padding(top = Dimens.spacingSmall),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            uiState.tipTranscription?.let { ipa ->
                                Text(
                                    text = stringResource(R.string.pronunciation_ipa_format, ipa),
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

                    TrainingActionRow(
                        onCheck = onCheck,
                        onNext = onNext,
                        awaitingNext = uiState.awaitingNext,
                        checkEnabled = uiState.canCheck,
                        onTip = onTipRequested.takeIf { uiState.canUseTip },
                        onSkip = onSkip.takeIf { uiState.canSkip },
                    )
                }
        }
    }
}

@LightDarkPreview
@Composable
private fun PronunciationScreenUnansweredPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            readsSentences = false,
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    word = "work",
                    recordingState = RecordingState.IDLE,
                ),
            onClose = {},
            onRecordRequested = {},
            onPlayRecording = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun PronunciationScreenRecordedPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            readsSentences = false,
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    word = "work",
                    recordingState = RecordingState.RECORDED,
                    recognizedText = "praca",
                    recordedAudioPath = "/cache/pronunciation_attempt.wav",
                ),
            onClose = {},
            onRecordRequested = {},
            onPlayRecording = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun PronunciationScreenTipRevealedPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            readsSentences = false,
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    word = "apple",
                    recordingState = RecordingState.IDLE,
                    tipLevel = 2,
                    tipTranslation = "jabłko",
                    tipTranscription = "jabuko",
                ),
            onClose = {},
            onRecordRequested = {},
            onPlayRecording = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun PronunciationScreenIncorrectPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            readsSentences = false,
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    word = "work",
                    recordingState = RecordingState.RECORDED,
                    recognizedText = "prace",
                    answerState = AnswerState.Incorrect("praca"),
                ),
            onClose = {},
            onRecordRequested = {},
            onPlayRecording = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
