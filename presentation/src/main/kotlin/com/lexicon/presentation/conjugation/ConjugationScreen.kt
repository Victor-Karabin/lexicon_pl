package com.lexicon.presentation.conjugation

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.conjugation.ConjugationAnswerMode
import com.lexicon.interactors.conjugation.ConjugationStep
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.presentation.R
import com.lexicon.presentation.common.ClueImage
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.presets.AddImageTile
import com.lexicon.presentation.presets.ImageCandidate
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.component.AnswerChip
import com.lexicon.presentation.theme.component.AnswerChipState
import com.lexicon.presentation.theme.component.ProgressDots
import kotlinx.collections.immutable.ImmutableList
import org.koin.androidx.compose.koinViewModel

private val PersonColumnWidth = 96.dp

object ConjugationTestTags {
    const val INFINITIVE = "conjugation_infinitive"
    const val TRANSLATION = "conjugation_translation"
    const val IMAGE = "conjugation_image"
    const val TRANSCRIPTION = "conjugation_transcription"
    const val EDIT = "conjugation_edit"
    const val IMAGE_PICKER = "conjugation_image_picker"
    const val BANK = "conjugation_bank"
    const val PROGRESS = "conjugation_progress"
    const val EMPTY = "conjugation_empty"

    fun person(label: String) = "conjugation_person_$label"

    fun option(value: String) = "conjugation_option_$value"

    fun play(label: String) = "conjugation_play_$label"
}

@Composable
fun ConjugationScreen(
    onSessionComplete: (Int, Int, Int, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConjugationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    ConjugationContent(
        uiState = uiState,
        onOptionPicked = viewModel::onOptionPicked,
        onRowCleared = viewModel::onRowCleared,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        onSpeak = viewModel::onSpeak,
        onEdit = viewModel::onEditVerb,
        onImageChosen = viewModel::onImageChosen,
        onImagePickerDismissed = viewModel::onImagePickerDismissed,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun ConjugationContent(
    uiState: ConjugationUiState,
    onOptionPicked: (String) -> Unit,
    onRowCleared: (GrammaticalPerson) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSpeak: (String) -> Unit,
    onEdit: () -> Unit,
    onImageChosen: (String) -> Unit,
    onImagePickerDismissed: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isPickingImage) {
        VerbImagePicker(
            choices = uiState.imageChoices,
            selected = uiState.question?.imageUrl,
            onChosen = onImageChosen,
            onDismiss = onImagePickerDismissed,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.conjugation_title), onClose = onClose) },
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
                        text = stringResource(R.string.conjugation_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(ConjugationTestTags.EMPTY),
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
                        ProgressDots(
                            step = uiState.stepIndex,
                            total = uiState.totalSteps,
                            modifier = Modifier.fillMaxWidth().testTag(ConjugationTestTags.PROGRESS),
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            ClueImage(
                                imageUrl = question.imageUrl,
                                fallbackText = question.infinitive,
                                modifier = Modifier.testTag(ConjugationTestTags.IMAGE),
                            )
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .testTag(ConjugationTestTags.EDIT),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.cards_edit),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Column {
                            Text(
                                text = question.infinitive,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag(ConjugationTestTags.INFINITIVE),
                            )
                            question.translation?.let { translation ->
                                Text(
                                    text = translation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag(ConjugationTestTags.TRANSLATION),
                                )
                            }
                            question.transcription?.let { ipa ->
                                Text(
                                    text = stringResource(R.string.pronunciation_ipa_format, ipa),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag(ConjugationTestTags.TRANSCRIPTION),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny)) {
                            question.steps.forEach { step ->
                                PersonRow(
                                    step = step,
                                    uiState = uiState,
                                    onCleared = { onRowCleared(step.variant.person) },
                                    onSpeak = onSpeak,
                                )
                            }
                        }

                        if (!uiState.isAnswered) {
                            OptionBank(uiState = uiState, onOptionPicked = onOptionPicked)
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

@Composable
private fun PersonRow(
    step: ConjugationStep,
    uiState: ConjugationUiState,
    onCleared: () -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val person = step.variant.person
    val chosen = uiState.answers[person]
    val isRight = uiState.correctness[person]

    val filled = when {
        chosen == null -> stringResource(R.string.conjugation_blank)
        step.mode == ConjugationAnswerMode.ENDING -> chosen
        else -> chosen
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !uiState.isAnswered && chosen != null, onClick = onCleared)
            .testTag(ConjugationTestTags.person(person.label)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Text(
            text = person.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(PersonColumnWidth),
        )

        Text(
            text = if (step.mode == ConjugationAnswerMode.ENDING) step.stem + filled else filled,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = when (isRight) {
                true -> LexiconSuccess
                false -> LexiconError
                null -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )

        if (uiState.isAnswered) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.word_pronounce, step.spokenForm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onSpeak(step.spokenForm) }
                    .padding(Dimens.spacingSmall)
                    .testTag(ConjugationTestTags.play(person.label)),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionBank(
    uiState: ConjugationUiState,
    onOptionPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = uiState.question ?: return
    val used = uiState.usedOptions

    FlowRow(
        modifier = modifier.fillMaxWidth().testTag(ConjugationTestTags.BANK),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        question.bank.forEach { option ->
            AnswerChip(
                label = option,
                state = if (option in used) AnswerChipState.SELECTED else AnswerChipState.UNSELECTED,
                onClick = { onOptionPicked(option) }.takeIf { !uiState.isAnswered && option !in used },
                modifier = Modifier.testTag(ConjugationTestTags.option(option)),
            )
        }
    }
}

@Composable
private fun VerbImagePicker(
    choices: ImmutableList<String>,
    selected: String?,
    onChosen: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(ConjugationTestTags.IMAGE_PICKER),
        title = { Text(stringResource(R.string.create_word_image)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                AddImageTile(onPicked = onChosen)
                choices.forEach { url ->
                    ImageCandidate(url = url, isSelected = url == selected, onClick = { onChosen(url) })
                }
            }
        },
    )
}
