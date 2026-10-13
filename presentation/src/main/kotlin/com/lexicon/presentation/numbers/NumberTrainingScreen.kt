package com.lexicon.presentation.numbers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.course.AnswerInputs
import com.lexicon.presentation.course.TranscribeRow
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.ProgressDots
import org.koin.androidx.compose.koinViewModel

object NumberTrainingTestTags {
    const val NUMBER = "number_training_number"
    const val LISTEN = "number_training_listen"
    const val TRANSCRIPTION = "number_training_transcription"
    const val EMPTY = "number_training_empty"
}

@Composable
fun NumberTrainingScreen(
    onSessionComplete: (Int, Int, Int, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NumberTrainingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    AnswerInputs {
        Scaffold(
            modifier = modifier,
            topBar = { TrainingTopBar(title = stringResource(R.string.numbers_title), onClose = onClose) },
        ) { padding ->
            val question = uiState.question
            when {
                uiState.isLoading ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                question == null ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.numbers_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag(NumberTrainingTestTags.EMPTY),
                        )
                    }

                else ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
                        NumberQuestionBody(
                            uiState = uiState,
                            question = question,
                            onAnswerChanged = viewModel::onAnswerChanged,
                            onSpeak = viewModel::onSpeak,
                            modifier = Modifier.weight(1f),
                        )
                        TrainingActionRow(
                            onCheck = viewModel::onCheck,
                            onNext = viewModel::onNext,
                            awaitingNext = uiState.isAnswered,
                            checkEnabled = uiState.canCheck,
                        )
                    }
            }
        }
    }
}

@Composable
private fun NumberQuestionBody(
    uiState: NumberTrainingUiState,
    question: NumberQuestion,
    onAnswerChanged: (String) -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        ProgressDots(step = uiState.stepIndex, total = uiState.totalSteps, modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingLarge),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = question.digits,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false).testTag(NumberTrainingTestTags.NUMBER),
            )
            IconButton(onClick = onSpeak, modifier = Modifier.testTag(NumberTrainingTestTags.LISTEN)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.numbers_listen),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        val verdict = uiState.verdict
        TranscribeRow(
            item = TranscribeItem("", LessonAnswerChecker.closest(question.answers, uiState.answer)),
            value = uiState.answer,
            isCorrect = verdict?.let { it == AnswerVerdict.CORRECT },
            answerState = when (verdict) {
                null -> AnswerState.Unanswered
                AnswerVerdict.CORRECT -> AnswerState.Correct
                else -> AnswerState.Incorrect()
            },
            onValueChanged = onAnswerChanged,
            isAlmost = verdict == AnswerVerdict.ALMOST,
            prompt = stringResource(R.string.numbers_write_prompt),
            imeAction = ImeAction.Done,
        )

        if (uiState.isAnswered) {
            Text(
                text = stringResource(R.string.pronunciation_ipa_format, question.transcription),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(NumberTrainingTestTags.TRANSCRIPTION),
            )
        }
    }
}
