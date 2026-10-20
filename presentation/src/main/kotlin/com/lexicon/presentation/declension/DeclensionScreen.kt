package com.lexicon.presentation.declension

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.presentation.R
import com.lexicon.presentation.common.ClueImage
import com.lexicon.presentation.common.ExampleSentenceRow
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.labelId
import com.lexicon.presentation.conjugation.DrillImageHeight
import com.lexicon.presentation.conjugation.PersonFormRow
import com.lexicon.presentation.conjugation.VerbHeading
import com.lexicon.presentation.course.AnswerInputs
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.ProgressDots
import org.koin.androidx.compose.koinViewModel

private val CaseColumnWidth = 112.dp

@Composable
fun DeclensionScreen(
    onSessionComplete: (Int, Int, Int, Int) -> Unit,
    onOpenRules: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeclensionViewModel = koinViewModel(),
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

    DeclensionContent(
        uiState = uiState,
        onAnswerChanged = viewModel::onAnswerChanged,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        onSpeak = viewModel::onSpeak,
        onOpenRules = onOpenRules,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun DeclensionContent(
    uiState: DeclensionUiState,
    onAnswerChanged: (GrammaticalCase, String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSpeak: (String) -> Unit,
    onOpenRules: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnswerInputs {
        Scaffold(
            modifier = modifier,
            topBar = {
                TrainingTopBar(
                    title = stringResource(R.string.declension_title),
                    onClose = onClose,
                    actions = { CasesRuleButton(onClick = onOpenRules) },
                )
            },
        ) { padding ->
            val table = uiState.table

            when {
                uiState.isLoading ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                table == null ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(if (uiState.isComplete) R.string.declension_complete else R.string.declension_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                else ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(Dimens.spacingMedium),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                        ) {
                            ProgressDots(step = uiState.stepIndex, total = uiState.totalSteps, modifier = Modifier.fillMaxWidth())

                            ClueImage(imageUrl = table.imageUrl, fallbackText = table.word, height = DrillImageHeight)

                            VerbHeading(infinitive = table.word, translation = table.translation) {
                                Text(
                                    text = wordKindLabel(table.partOfSpeech, table.gender),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                table.transcription?.let { ipa ->
                                    Text(
                                        text = stringResource(R.string.pronunciation_ipa_format, ipa),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (uiState.isAnswered && table.example.isNotBlank()) {
                                    val example = ExampleSentence.of(table.example, word = table.word)
                                    ExampleSentenceRow(
                                        example = example,
                                        onPlay = { onSpeak(example.text) },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Text(
                                text = stringResource(table.column.labelId()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                                table.steps.forEachIndexed { index, step ->
                                    val case = step.variant.case
                                    val typed = uiState.answers[case].orEmpty()
                                    PersonFormRow(
                                        person = stringResource(case.labelId()),
                                        typed = typed,
                                        expected = LessonAnswerChecker.closest(step.forms, typed),
                                        verdict = uiState.verdicts[case],
                                        onAnswerChanged = { onAnswerChanged(case, it) },
                                        onSpeak = { onSpeak(step.spokenForm) },
                                        imeAction = if (index == table.steps.lastIndex) ImeAction.Done else ImeAction.Next,
                                        labelWidth = CaseColumnWidth,
                                    )
                                }
                            }
                        }

                        TrainingActionRow(
                            onCheck = onCheck,
                            onNext = onNext,
                            awaitingNext = uiState.isAnswered,
                            checkEnabled = uiState.canCheck,
                        )
                    }
            }
        }
    }
}
