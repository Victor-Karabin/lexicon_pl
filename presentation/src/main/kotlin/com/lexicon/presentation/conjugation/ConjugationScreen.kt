package com.lexicon.presentation.conjugation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.presentation.R
import com.lexicon.presentation.common.ClueImage
import com.lexicon.presentation.common.ExampleSentenceRow
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.aspectLabel
import com.lexicon.presentation.course.AnswerInputs
import com.lexicon.presentation.presets.ImagePickerDialog
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.ProgressDots
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

object ConjugationTestTags {
    const val INFINITIVE = "conjugation_infinitive"
    const val TRANSLATION = "conjugation_translation"
    const val IMAGE = "conjugation_image"
    const val TRANSCRIPTION = "conjugation_transcription"
    const val EDIT = "conjugation_edit"
    const val IMAGE_PICKER = "conjugation_image_picker"
    const val PROGRESS = "conjugation_progress"
    const val EMPTY = "conjugation_empty"

    fun person(label: String) = "conjugation_person_$label"
}

@Composable
fun ConjugationScreen(
    onSessionComplete: (Int, Int, Int, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConjugationViewModel = koinViewModel(),
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

    ConjugationContent(
        uiState = uiState,
        onAnswerChanged = viewModel::onAnswerChanged,
        onCheck = viewModel::onCheck,
        onNext = viewModel::onNext,
        onSpeak = viewModel::onSpeak,
        onEdit = viewModel::onEditVerb,
        onImageChosen = viewModel::onImageChosen,
        onMoreVerbImages = viewModel::onMoreVerbImages,
        onImagePickerDismissed = viewModel::onImagePickerDismissed,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun ConjugationContent(
    uiState: ConjugationUiState,
    onAnswerChanged: (GrammaticalPerson, String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSpeak: (String) -> Unit,
    onEdit: () -> Unit,
    onImageChosen: (String) -> Unit,
    onMoreVerbImages: () -> Unit,
    onImagePickerDismissed: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isPickingImage) {
        ImagePickerDialog(
            candidates = uiState.imageChoices,
            ownImages = persistentListOf(),
            selected = uiState.table?.imageUrl,
            isLoading = uiState.isLoadingImages,
            canLoadMore = uiState.hasMoreImages,
            onSelected = onImageChosen,
            onOwnImageAdded = onImageChosen,
            onLoadMore = onMoreVerbImages,
            onDismiss = onImagePickerDismissed,
        )
    }

    AnswerInputs {
        Scaffold(
            modifier = modifier,
            topBar = { TrainingTopBar(title = stringResource(R.string.conjugation_title), onClose = onClose) },
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
                            text = stringResource(if (uiState.isComplete) R.string.conjugation_complete else R.string.conjugation_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag(ConjugationTestTags.EMPTY),
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
                            ProgressDots(
                                step = uiState.stepIndex,
                                total = uiState.totalSteps,
                                modifier = Modifier.fillMaxWidth().testTag(ConjugationTestTags.PROGRESS),
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                ClueImage(
                                    imageUrl = table.imageUrl,
                                    fallbackText = table.infinitive,
                                    height = DrillImageHeight,
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

                            VerbHeading(infinitive = table.infinitive, translation = table.translation) {
                                table.aspect?.let { aspect ->
                                    Text(
                                        text = aspectLabel(aspect),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                table.transcription?.let { ipa ->
                                    Text(
                                        text = stringResource(R.string.pronunciation_ipa_format, ipa),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag(ConjugationTestTags.TRANSCRIPTION),
                                    )
                                }

                                if (uiState.isAnswered) {
                                    val example = ExampleSentence.of(table.example, word = table.infinitive)
                                    ExampleSentenceRow(
                                        example = example,
                                        onPlay = { onSpeak(example.text) },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                                table.steps.forEachIndexed { index, step ->
                                    val person = step.variant.person
                                    val typed = uiState.answers[person].orEmpty()
                                    PersonFormRow(
                                        person = person.label,
                                        typed = typed,
                                        expected = LessonAnswerChecker.closest(step.forms, typed),
                                        verdict = uiState.verdicts[person],
                                        onAnswerChanged = { onAnswerChanged(person, it) },
                                        onSpeak = { onSpeak(step.spokenForm) },
                                        imeAction = if (index == table.steps.lastIndex) ImeAction.Done else ImeAction.Next,
                                        modifier = Modifier.testTag(ConjugationTestTags.person(person.label)),
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
