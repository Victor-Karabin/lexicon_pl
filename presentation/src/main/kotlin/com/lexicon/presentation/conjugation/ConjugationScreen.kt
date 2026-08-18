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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.lexicon.interactors.conjugation.ConjugationAnswerMode
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.ClueImage
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.presets.AddImageTile
import com.lexicon.presentation.presets.ImageCandidate
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.component.AnswerChip
import com.lexicon.presentation.theme.component.AnswerChipState
import com.lexicon.presentation.theme.component.PlayButton
import kotlinx.collections.immutable.ImmutableList
import org.koin.androidx.compose.koinViewModel

object ConjugationTestTags {
    const val INFINITIVE = "conjugation_infinitive"
    const val PERSON = "conjugation_person"
    const val PROMPT = "conjugation_prompt"
    const val IMAGE = "conjugation_image"
    const val TRANSCRIPTION = "conjugation_transcription"
    const val PLAY = "conjugation_play"
    const val PROGRESS = "conjugation_progress"
    const val STATUS = "conjugation_status"
    const val EMPTY = "conjugation_empty"
    const val COMPLETE = "conjugation_complete"
    const val EDIT_IMAGE = "conjugation_edit_image"
    const val IMAGE_PICKER = "conjugation_image_picker"

    fun option(value: String) = "conjugation_option_$value"
}

@Composable
fun ConjugationScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConjugationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ConjugationContent(
        uiState = uiState,
        onOptionSelected = viewModel::onOptionSelected,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        onSpeak = viewModel::onSpeak,
        onEditImage = viewModel::onEditImage,
        onImageChosen = viewModel::onImageChosen,
        onImagePickerDismissed = viewModel::onImagePickerDismissed,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun ConjugationContent(
    uiState: ConjugationUiState,
    onOptionSelected: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSpeak: () -> Unit,
    onEditImage: () -> Unit,
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

            uiState.hasNoVerbs ->
                Message(text = stringResource(R.string.conjugation_none), tag = ConjugationTestTags.EMPTY, padding = padding)

            question == null ->
                Message(
                    text = stringResource(R.string.conjugation_complete),
                    tag = ConjugationTestTags.COMPLETE,
                    padding = padding,
                )

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
                        Progress(uiState)
                        Prompt(uiState, onSpeak, onEditImage)
                        Options(uiState, onOptionSelected)
                        Status(uiState)
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
private fun Message(
    text: String,
    tag: String,
    padding: androidx.compose.foundation.layout.PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun Progress(
    uiState: ConjugationUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny)) {
        Text(
            text = stringResource(
                R.string.conjugation_progress,
                uiState.progress.mastered,
                uiState.progress.total,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(ConjugationTestTags.PROGRESS),
        )
        LinearProgressIndicator(progress = { uiState.progress.fraction }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Prompt(
    uiState: ConjugationUiState,
    onSpeak: () -> Unit,
    onEditImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = uiState.question ?: return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        ClueImage(
            imageUrl = question.imageUrl,
            fallbackText = question.variant.infinitive,
            modifier = Modifier.clickable(onClick = onEditImage).testTag(ConjugationTestTags.IMAGE),
        )
        TextButton(onClick = onEditImage, modifier = Modifier.testTag(ConjugationTestTags.EDIT_IMAGE)) {
            Text(stringResource(R.string.conjugation_change_image))
        }

        Text(
            text = question.variant.infinitive,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(ConjugationTestTags.INFINITIVE),
        )

        Text(
            text = question.variant.person.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag(ConjugationTestTags.PERSON),
        )

        if (question.mode == ConjugationAnswerMode.ENDING) {
            Text(
                text = question.stem + stringResource(R.string.conjugation_blank),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag(ConjugationTestTags.PROMPT),
            )
        }

        Text(
            text = stringResource(
                if (question.mode == ConjugationAnswerMode.ENDING) {
                    R.string.conjugation_ending_hint
                } else {
                    R.string.conjugation_form_hint
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        question.transcription?.let { ipa ->
            Text(
                text = stringResource(R.string.pronunciation_ipa_format, ipa),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(ConjugationTestTags.TRANSCRIPTION),
            )
        }

        if (uiState.isAnswered) {
            PlayButton(
                onClick = onSpeak,
                label = question.spokenForm,
                modifier = Modifier.testTag(ConjugationTestTags.PLAY),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Options(
    uiState: ConjugationUiState,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = uiState.question ?: return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        question.options.forEach { option ->
            AnswerChip(
                label = option,
                state = optionState(uiState, option),
                onClick = { onOptionSelected(option) }.takeIf { !uiState.isAnswered },
                modifier = Modifier.testTag(ConjugationTestTags.option(option)),
            )
        }
    }
}

private fun optionState(
    uiState: ConjugationUiState,
    option: String,
): AnswerChipState {
    val question = uiState.question ?: return AnswerChipState.UNSELECTED
    val isCorrect = question.correctOptions.any { it.equals(option, ignoreCase = true) }

    return when {
        !uiState.isAnswered -> if (option == uiState.selected) AnswerChipState.SELECTED else AnswerChipState.UNSELECTED
        isCorrect -> AnswerChipState.CORRECT
        option == uiState.selected -> AnswerChipState.INCORRECT
        else -> AnswerChipState.UNSELECTED
    }
}

@Composable
private fun Status(
    uiState: ConjugationUiState,
    modifier: Modifier = Modifier,
) {
    val label = when (uiState.answerState) {
        is AnswerState.Correct -> stringResource(R.string.status_correct)
        is AnswerState.Incorrect -> stringResource(R.string.status_incorrect)
        else -> return
    }

    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (uiState.answerState is AnswerState.Correct) LexiconSuccess else LexiconError,
        modifier = modifier.testTag(ConjugationTestTags.STATUS),
    )
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
        title = { Text(stringResource(R.string.conjugation_change_image)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        },
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
