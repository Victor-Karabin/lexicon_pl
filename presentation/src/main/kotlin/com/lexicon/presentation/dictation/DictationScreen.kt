package com.lexicon.presentation.dictation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.PlayButton
import com.lexicon.presentation.theme.component.ProgressDots
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictationScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val loadedState = uiState as? DictationUiState.Loaded
    LaunchedEffect(loadedState?.stepIndex, loadedState != null) {
        if (loadedState != null && loadedState.isEditable) {
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    DictationScreenContent(
        uiState = uiState,
        onClose = onClose,
        onAnswerChanged = viewModel::onAnswerChanged,
        onReplayAudio = viewModel::onReplayAudio,
        onTipRequested = viewModel::onTipRequested,
        onSkip = viewModel::onSkip,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        modifier = modifier,
        focusRequester = focusRequester,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DictationScreenContent(
    uiState: DictationUiState,
    onClose: () -> Unit,
    onAnswerChanged: (String) -> Unit,
    onReplayAudio: () -> Unit,
    onTipRequested: () -> Unit,
    onSkip: () -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.dictation_title), onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is DictationUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is DictationUiState.Loaded ->
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
                            onClick = onReplayAudio,
                            label = stringResource(R.string.action_listen_again),
                            modifier = Modifier.padding(top = Dimens.spacingLarge),
                        )

                        val fieldColors = when (uiState.answerState) {
                            is AnswerState.Correct ->
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LexiconSuccess,
                                    unfocusedBorderColor = LexiconSuccess,
                                    focusedLabelColor = LexiconSuccess,
                                    unfocusedLabelColor = LexiconSuccess,
                                )
                            is AnswerState.Incorrect, is AnswerState.Skipped ->
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LexiconError,
                                    unfocusedBorderColor = LexiconError,
                                    focusedLabelColor = LexiconError,
                                    unfocusedLabelColor = LexiconError,
                                )
                            is AnswerState.Unanswered -> OutlinedTextFieldDefaults.colors()
                        }

                        OutlinedTextField(
                            value = uiState.answerText,
                            onValueChange = onAnswerChanged,
                            enabled = true,
                            readOnly = !uiState.isEditable,
                            singleLine = true,
                            colors = fieldColors,
                            shape = LexiconShapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimens.spacingMedium)
                                .focusRequester(focusRequester),
                            label = { Text(stringResource(R.string.dictation_type_what_you_heard)) },
                        )

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
private fun DictationScreenUnansweredPreview() {
    LexiconTheme {
        DictationScreenContent(
            uiState = DictationUiState.Loaded(stepIndex = 2, totalSteps = 10, answerText = "prac"),
            onClose = {},
            onAnswerChanged = {},
            onReplayAudio = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun DictationScreenCorrectPreview() {
    LexiconTheme {
        DictationScreenContent(
            uiState =
                DictationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    answerText = "praca",
                    answerState = AnswerState.Correct,
                ),
            onClose = {},
            onAnswerChanged = {},
            onReplayAudio = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun DictationScreenIncorrectPreview() {
    LexiconTheme {
        DictationScreenContent(
            uiState =
                DictationUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    answerText = "prace",
                    answerState = AnswerState.Incorrect("praca"),
                ),
            onClose = {},
            onAnswerChanged = {},
            onReplayAudio = {},
            onTipRequested = {},
            onSkip = {},
            onCheck = {},
            onNext = {},
        )
    }
}
