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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.interactors.numbers.NumberGender
import com.lexicon.interactors.numbers.NumberKind
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.interactors.numbers.NumberRule
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.text
import com.lexicon.presentation.course.AnswerInputs
import com.lexicon.presentation.course.TranscribeRow
import com.lexicon.presentation.course.richText
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.ProgressDots
import org.koin.androidx.compose.koinViewModel

object NumberTrainingTestTags {
    const val NUMBER = "number_training_number"
    const val LISTEN = "number_training_listen"
    const val TRANSCRIPTION = "number_training_transcription"
    const val RULES = "number_training_rules"
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
                            onCheck = viewModel::onCheck,
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
    onCheck: () -> Unit,
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
                text = question.label().text(),
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
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(uiState.stepIndex) {
            focus.requestFocus()
            keyboard?.show()
        }
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
            prompt = stringResource(question.prompt()),
            imeAction = ImeAction.Done,
            onDone = onCheck,
            focusRequester = focus,
            reservesInfoSlot = false,
        )

        if (uiState.isAnswered) {
            Text(
                text = "[${question.transcription}]",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(NumberTrainingTestTags.TRANSCRIPTION),
            )
            NumberRules(rules = question.rules)
        }
    }
}

@Composable
private fun NumberRules(rules: List<NumberRule>) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(NumberTrainingTestTags.RULES),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
    ) {
        Text(
            text = stringResource(R.string.numbers_rules_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        rules.forEach { rule ->
            Text(text = richText(stringResource(rule.text())), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun NumberQuestion.prompt(): Int =
    when (kind) {
        NumberKind.CARDINAL ->
            when (gender) {
                null -> R.string.numbers_write_prompt
                NumberGender.MASCULINE -> R.string.numbers_write_masculine_prompt
                NumberGender.FEMININE -> R.string.numbers_write_feminine_prompt
                NumberGender.NEUTER -> R.string.numbers_write_neuter_prompt
            }
        NumberKind.ORDINAL ->
            when (gender) {
                NumberGender.FEMININE -> R.string.numbers_write_ordinal_feminine_prompt
                NumberGender.NEUTER -> R.string.numbers_write_ordinal_neuter_prompt
                else -> R.string.numbers_write_ordinal_prompt
            }
        NumberKind.ENUMERATION -> R.string.numbers_write_enumeration_prompt
    }

private fun NumberRule.text(): Int =
    when (this) {
        NumberRule.UNITS -> R.string.numbers_rule_units
        NumberRule.TEENS -> R.string.numbers_rule_teens
        NumberRule.TENS_TO_FORTY -> R.string.numbers_rule_tens_to_forty
        NumberRule.TENS_FROM_FIFTY -> R.string.numbers_rule_tens_from_fifty
        NumberRule.TENS_AND_UNITS -> R.string.numbers_rule_tens_and_units
        NumberRule.HUNDREDS -> R.string.numbers_rule_hundreds
        NumberRule.MILLION -> R.string.numbers_rule_million
        NumberRule.MILLIONS_FEW -> R.string.numbers_rule_millions_few
        NumberRule.MILLIONS_MANY -> R.string.numbers_rule_millions_many
        NumberRule.THOUSAND -> R.string.numbers_rule_thousand
        NumberRule.THOUSANDS_FEW -> R.string.numbers_rule_thousands_few
        NumberRule.THOUSANDS_MANY -> R.string.numbers_rule_thousands_many
        NumberRule.ORDINAL_UNITS -> R.string.numbers_rule_ordinal_units
        NumberRule.ORDINAL_TEENS -> R.string.numbers_rule_ordinal_teens
        NumberRule.ORDINAL_TENS -> R.string.numbers_rule_ordinal_tens
        NumberRule.ORDINAL_TENS_AND_UNITS -> R.string.numbers_rule_ordinal_tens_and_units
        NumberRule.ORDINAL_HUNDREDS -> R.string.numbers_rule_ordinal_hundreds
        NumberRule.ORDINAL_THOUSAND -> R.string.numbers_rule_ordinal_thousand
        NumberRule.ORDINAL_LAST_PART -> R.string.numbers_rule_ordinal_last_part
        NumberRule.ENUMERATION -> R.string.numbers_rule_enumeration
        NumberRule.ORDINAL_GENDERS -> R.string.numbers_rule_ordinal_genders
        NumberRule.ONE_GENDERS -> R.string.numbers_rule_one_genders
        NumberRule.TWO_GENDERS -> R.string.numbers_rule_two_genders
    }
