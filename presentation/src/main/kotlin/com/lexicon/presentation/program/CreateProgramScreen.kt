package com.lexicon.presentation.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.main.programTrainings
import com.lexicon.presentation.main.trainingDisplayName
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionText
import com.lexicon.presentation.theme.component.StatChip
import com.lexicon.presentation.theme.component.TileSkin
import com.lexicon.presentation.theme.component.tileSkin
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

private val QueueMedallionSize = 32.dp
private val MoveIconSize = 20.dp

@Composable
fun CreateProgramScreen(
    onClose: () -> Unit,
    onCreated: () -> Unit,
    onGoToVocabulary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateProgramViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    // The program's name is the app's, not the learner's: there is one of them and it
    // is over the study set, so the copy lives here with the rest of the wording.
    val name = stringResource(R.string.program_default_name)
    val description = stringResource(R.string.create_program_scope)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onCreated()
    }

    CreateProgramContent(
        uiState = uiState,
        onClose = onClose,
        onGoToVocabulary = onGoToVocabulary,
        onNewWordsChanged = viewModel::onNewWordsChanged,
        onReviewWordsChanged = viewModel::onReviewWordsChanged,
        onTrainingAdded = viewModel::onTrainingAdded,
        onTurnRemoved = viewModel::onTurnRemoved,
        onMoveEarlier = viewModel::onMoveEarlier,
        onMoveLater = viewModel::onMoveLater,
        onSave = { viewModel.onSave(name = name, description = description) },
        onEnrolToggled = viewModel::onEnrolToggled,
        modifier = modifier,
    )
}

@Composable
private fun CreateProgramContent(
    uiState: CreateProgramUiState,
    onClose: () -> Unit,
    onGoToVocabulary: () -> Unit,
    onNewWordsChanged: (Int) -> Unit,
    onReviewWordsChanged: (Int) -> Unit,
    onTrainingAdded: (String) -> Unit,
    onTurnRemoved: (Int) -> Unit,
    onMoveEarlier: (Int) -> Unit,
    onMoveLater: (Int) -> Unit,
    onSave: () -> Unit,
    onEnrolToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrainingTopBar(
                title = stringResource(
                    if (uiState.isEditing) R.string.edit_program_title else R.string.create_program_title,
                ),
                onClose = onClose,
            )
        },
        bottomBar = {
            // Nothing to save when there is nothing to build a program over, and a
            // permanently greyed button is just furniture.
            if (uiState.hasFavourites) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                ) {
                    // A program that exists is a program that can be started, and
                    // this is the only screen it has.
                    if (uiState.isEditing) {
                        OutlinedButton(onClick = onEnrolToggled, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    if (uiState.isEnrolled) R.string.program_leave else R.string.program_start,
                                ),
                            )
                        }
                    }
                    // A greyed Save with no reason beside it reads as a broken
                    // button. The queue is the one thing that can hold it back.
                    if (uiState.queue.isEmpty()) {
                        Text(
                            text = stringResource(R.string.create_program_needs_training),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    TextButton(
                        onClick = onSave,
                        enabled = uiState.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.create_save))
                    }
                }
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!uiState.hasFavourites) {
            NoFavourites(onGoToVocabulary = onGoToVocabulary, modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            val skin = tileSkin()

            // What the program will teach, said before anything is asked, because a
            // program over an empty study set cannot be built at all.
            StudySetCard(uiState = uiState, skin = skin)

            AmountSlider(
                label = stringResource(R.string.create_program_new_words),
                value = uiState.newWordsPerDay,
                range = MIN_NEW_WORDS_A_DAY..uiState.maxNewWords,
                onChange = onNewWordsChanged,
            )
            AmountSlider(
                label = stringResource(R.string.create_program_review_words),
                value = uiState.reviewWordsPerDay,
                range = 0..uiState.maxNewWords,
                onChange = onReviewWordsChanged,
            )

            SectionLabel(stringResource(R.string.create_program_trainings))
            TrainingPicker(onAdd = onTrainingAdded)

            SectionLabel(stringResource(R.string.create_program_queue))
            if (uiState.queue.isEmpty()) {
                Text(
                    text = stringResource(R.string.create_program_queue_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.create_program_queue_note, uiState.queue.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.queue.forEachIndexed { index, training ->
                    QueueRow(
                        position = index + 1,
                        training = training,
                        isFirst = index == 0,
                        isLast = index == uiState.queue.lastIndex,
                        skin = skin,
                        onMoveEarlier = { onMoveEarlier(index) },
                        onMoveLater = { onMoveLater(index) },
                        onRemove = { onTurnRemoved(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StudySetCard(
    uiState: CreateProgramUiState,
    skin: TileSkin,
) {
    GradientTile(skin = skin) {
        Text(
            text = stringResource(R.string.create_program_scope),
            style = MaterialTheme.typography.bodyMedium,
            color = skin.onTile,
        )
        StatChip(
            icon = Icons.Default.Translate,
            text = stringResource(R.string.plan_program_words, uiState.favourites),
            skin = skin,
        )
    }
}

/**
 * Nothing starred, so there is no program to build.
 *
 * The form is not shown at all rather than shown and refused: every field on it
 * would be a choice about words that do not exist yet, and the only useful thing to
 * do from here is go and star some.
 */
@Composable
private fun NoFavourites(
    onGoToVocabulary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.spacingXl),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.create_program_no_favourites),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGoToVocabulary) {
            Text(stringResource(R.string.training_go_to_vocabulary))
        }
    }
}

@Composable
private fun AmountSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = value.coerceIn(range).toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            // A slider with no steps is a slider that cannot land on a round number.
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

/**
 * Every training a program may use, as something to add a turn at.
 *
 * Adds rather than toggles: picking the same one twice is how a day gets two turns at
 * it, so the chips do not carry a chosen state — the queue below is the answer to
 * what was chosen and how often.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingPicker(onAdd: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        programTrainings.forEach { entry ->
            AssistChip(
                onClick = { onAdd(entry.id) },
                label = { Text(entry.displayName) },
                leadingIcon = {
                    Icon(imageVector = entry.icon, contentDescription = null, modifier = Modifier.size(MoveIconSize))
                },
            )
        }
    }
}

@Composable
private fun QueueRow(
    position: Int,
    training: String,
    isFirst: Boolean,
    isLast: Boolean,
    skin: TileSkin,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onRemove: () -> Unit,
) {
    GradientTile(skin = skin) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(skin = skin, size = QueueMedallionSize) { MedallionText("$position", skin) }
            Text(
                text = trainingDisplayName(training),
                style = MaterialTheme.typography.bodyLarge,
                color = skin.onTile,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMoveEarlier, enabled = !isFirst) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.create_program_move_earlier),
                    tint = skin.onTile,
                )
            }
            IconButton(onClick = onMoveLater, enabled = !isLast) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.create_program_move_later),
                    tint = skin.onTile,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.create_program_remove_turn),
                    tint = skin.onTile,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@LightDarkPreview
@Composable
private fun CreateProgramNoFavouritesPreview() {
    LexiconTheme {
        CreateProgramContent(
            uiState = CreateProgramUiState(isLoading = false, favourites = 0),
            onClose = {},
            onGoToVocabulary = {},
            onNewWordsChanged = {},
            onReviewWordsChanged = {},
            onTrainingAdded = {},
            onTurnRemoved = {},
            onMoveEarlier = {},
            onMoveLater = {},
            onSave = {},
            onEnrolToggled = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CreateProgramPreview() {
    LexiconTheme {
        CreateProgramContent(
            uiState = CreateProgramUiState(
                isLoading = false,
                favourites = 84,
                queue = persistentListOf("word_match", "dictation", "word_match", "true_or_false"),
            ),
            onClose = {},
            onGoToVocabulary = {},
            onNewWordsChanged = {},
            onReviewWordsChanged = {},
            onTrainingAdded = {},
            onTurnRemoved = {},
            onMoveEarlier = {},
            onMoveLater = {},
            onSave = {},
            onEnrolToggled = {},
        )
    }
}
