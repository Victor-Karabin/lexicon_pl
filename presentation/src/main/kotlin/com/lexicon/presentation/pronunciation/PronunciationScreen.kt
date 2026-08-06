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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.presentation.R
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

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
        topBar = { TrainingTopBar(title = "Pronunciation Check", onClose = onClose) },
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
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium)) {
                    LinearProgressIndicator(
                        progress = { (uiState.stepIndex + 1f) / uiState.totalSteps },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${uiState.stepIndex + 1} / ${uiState.totalSteps}",
                        modifier = Modifier.padding(top = Dimens.spacingSmall),
                        style = MaterialTheme.typography.labelMedium,
                    )

                    TextButton(onClick = onReplayReferenceAudio, modifier = Modifier.padding(top = Dimens.spacingLarge)) {
                        Text("🔊 Listen to reference")
                    }

                    Button(
                        onClick = onRecordRequested,
                        enabled = uiState.canRecord,
                        modifier = Modifier.padding(top = Dimens.spacingMedium),
                    ) {
                        Text(
                            when (uiState.recordingState) {
                                RecordingState.IDLE -> "🎤 Record"
                                RecordingState.RECORDING -> "Listening…"
                                RecordingState.PROCESSING -> "Checking…"
                            },
                        )
                    }

                    uiState.recognizedText?.let { recognized ->
                        Text(
                            text = "Heard: $recognized",
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

                    uiState.revealedAnswer?.let { answer ->
                        Text(
                            text = stringResource(R.string.expected_format, answer),
                            modifier = Modifier.padding(top = Dimens.spacingSmall),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    ) {
                        TextButton(onClick = onTipRequested, enabled = uiState.canUseTip) {
                            Text("Tip")
                        }
                        TextButton(onClick = onSkip, enabled = uiState.canSkip) {
                            Text("Skip")
                        }
                        if (uiState.awaitingNext) {
                            Button(onClick = onNext) {
                                Text("Next")
                            }
                        }
                    }
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PronunciationScreenPreview() {
    LexiconTheme {
        PronunciationScreenContent(
            uiState =
                PronunciationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    recordingState = RecordingState.IDLE,
                    recognizedText = "prace",
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
