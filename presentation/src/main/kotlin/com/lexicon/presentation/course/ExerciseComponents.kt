package com.lexicon.presentation.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.course.GAP_MARKER
import com.lexicon.interactors.course.GapFillItem
import com.lexicon.interactors.course.LETTER_GAP
import com.lexicon.interactors.course.LetterFillItem
import com.lexicon.interactors.course.MatchItem
import com.lexicon.interactors.course.MinimalPairItem
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.answerStateColor
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.component.AnswerChip
import com.lexicon.presentation.theme.component.AnswerChipState
import com.lexicon.presentation.theme.component.AnswerChipVariant

private val PlayIconSize = 20.dp
private val LetterCellSize = 36.dp
private val MatchIconSize = 32.dp

private val GapCharacterWidth = 11.dp
private val GapMinWidth = 56.dp
private val GapMaxWidth = 200.dp

private val exerciseIcons = mapOf(
    "repeat" to Icons.Default.Repeat,
    "spell" to Icons.Default.Abc,
    "read" to Icons.AutoMirrored.Filled.MenuBook,
    "write" to Icons.Default.Edit,
    "listen" to Icons.AutoMirrored.Filled.VolumeUp,
    "speak" to Icons.Default.RecordVoiceOver,
)

@Composable
fun ExerciseAudioButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(PlayIconSize),
        )
        Text(
            text = stringResource(if (isPlaying) R.string.exercise_pause else R.string.exercise_play),
            modifier = Modifier.padding(start = Dimens.spacingSmall),
        )
    }
}

@Composable
fun MinimalPairRow(
    item: MinimalPairItem,
    selected: String?,
    answerState: AnswerState,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall)) {
        ItemLabel(item.label)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            item.options.forEach { option ->
                AnswerChip(
                    label = option,
                    modifier = Modifier.weight(1f),
                    state = choiceState(
                        option = option,
                        selected = selected,
                        answer = item.answer,
                        answerState = answerState,
                    ),
                    onClick = { onSelect(option) }.takeIf { answerState is AnswerState.Unanswered },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GapFillRow(
    item: GapFillItem,
    values: List<String>,
    correctness: List<Boolean>,
    answerState: AnswerState,
    onValueChanged: (Int, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall)) {
        item.speaker?.let { speaker ->
            Text(
                text = speaker,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
        ) {
            var gap = 0
            item.prompt.split(GAP_MARKER).forEachIndexed { index, fragment ->

                if (index > 0) {
                    val at = gap++
                    InlineGap(
                        value = values.getOrElse(at) { "" },
                        expected = item.answers.getOrElse(at) { "" },
                        isCorrect = correctness.getOrNull(at),
                        answerState = answerState,
                        onValueChanged = { onValueChanged(at, it) },
                    )
                }
                if (fragment.isNotBlank()) {
                    Text(
                        text = fragment.trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = Dimens.spacingSmall),
                    )
                }
            }
        }

        if (answerState !is AnswerState.Unanswered && correctness.any { !it }) {
            Text(
                text = item.answers.foldIndexed(item.prompt) { index, line, answer ->
                    line.replaceFirst(GAP_MARKER, item.answers.getOrElse(index) { answer })
                },
                style = MaterialTheme.typography.bodyMedium,
                color = LexiconSuccess,
                modifier = Modifier.padding(top = Dimens.spacingTiny),
            )
        }
    }
}

@Composable
fun TranscribeRow(
    item: TranscribeItem,
    value: String,
    isCorrect: Boolean?,
    answerState: AnswerState,
    onValueChanged: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingTiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Text(
            text = "${item.label})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            singleLine = true,
            enabled = answerState is AnswerState.Unanswered,
            isError = isCorrect == false,
            modifier = Modifier.weight(1f),
            shape = LexiconShapes.small,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            supportingText = {
                if (isCorrect == false) {
                    Text(
                        text = stringResource(R.string.expected_format, item.answer),
                        color = answerStateColor(answerState),
                    )
                }
            },
        )
    }
}

@Composable
fun MatchBoard(
    items: List<MatchItem>,
    choices: List<String>,
    values: List<String>,
    correctness: List<Boolean>,
    answerState: AnswerState,
    selectedPrompt: Int?,
    onPromptSelected: (Int) -> Unit,
    onChoiceSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            items.forEachIndexed { index, item ->
                MatchPrompt(
                    item = item,
                    chosen = values.getOrElse(index) { "" },
                    isCorrect = correctness.getOrNull(index),
                    isSelected = selectedPrompt == index,
                    enabled = answerState is AnswerState.Unanswered,
                    onClick = { onPromptSelected(index) },
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            choices.forEachIndexed { index, choice ->
                AnswerChip(
                    label = "${('A' + index)}. $choice",
                    variant = AnswerChipVariant.ROW,
                    state = if (choice in values) AnswerChipState.SELECTED else AnswerChipState.UNSELECTED,
                    onClick = { onChoiceSelected(choice) }.takeIf {
                        answerState is AnswerState.Unanswered && selectedPrompt != null
                    },
                )
            }
        }
    }
}

@Composable
private fun MatchPrompt(
    item: MatchItem,
    chosen: String,
    isCorrect: Boolean?,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val border = when {
        isCorrect == true -> LexiconSuccess
        isCorrect == false -> LexiconError
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, border, LexiconShapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Dimens.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            if (item.label.isNotBlank()) {
                Text(
                    text = "${item.label}.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.iconName?.let { name ->
                Icon(
                    imageVector = exerciseIcons[name] ?: Icons.Default.QuestionMark,
                    contentDescription = null,
                    modifier = Modifier.size(MatchIconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } ?: Text(text = item.prompt, style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            text = chosen.ifBlank { " " },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterFillRow(
    item: LetterFillItem,
    values: List<String>,
    correctness: List<Boolean>,
    answerState: AnswerState,
    onValueChanged: (Int, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall)) {
        ItemLabel(item.label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
        ) {
            var gap = 0
            item.pattern.forEach { character ->
                when (character) {
                    LETTER_GAP -> {
                        val at = gap++
                        LetterCell(
                            value = values.getOrElse(at) { "" },
                            isCorrect = correctness.getOrNull(at),
                            enabled = answerState is AnswerState.Unanswered,
                            onValueChanged = { onValueChanged(at, it) },
                        )
                    }

                    ' ' -> Box(modifier = Modifier.width(Dimens.spacingMedium).height(LetterCellSize))

                    else ->
                        Box(
                            modifier = Modifier.height(LetterCellSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = character.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                }
            }
        }
        if (answerState !is AnswerState.Unanswered && correctness.any { !it }) {
            Text(
                text = item.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = LexiconSuccess,
                modifier = Modifier.padding(top = Dimens.spacingTiny),
            )
        }
    }
}

@Composable
private fun LetterCell(
    value: String,
    isCorrect: Boolean?,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
) {
    val border = when (isCorrect) {
        true -> LexiconSuccess
        false -> LexiconError
        null -> MaterialTheme.colorScheme.outline
    }

    BasicTextField(
        value = value,
        onValueChange = { onValueChanged(it.takeLast(1)) },
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { field ->
            Box(
                modifier = Modifier
                    .size(LetterCellSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, LexiconShapes.small)
                    .border(2.dp, border, LexiconShapes.small),
                contentAlignment = Alignment.Center,
                content = { field() },
            )
        },
    )
}

@Composable
private fun InlineGap(
    value: String,
    expected: String,
    isCorrect: Boolean?,
    answerState: AnswerState,
    onValueChanged: (String) -> Unit,
) {
    val underline = when (isCorrect) {
        true -> LexiconSuccess
        false -> LexiconError
        null -> MaterialTheme.colorScheme.outline
    }
    val width = (GapCharacterWidth * expected.length).coerceIn(GapMinWidth, GapMaxWidth)

    BasicTextField(
        value = value,
        onValueChange = onValueChanged,
        enabled = answerState is AnswerState.Unanswered,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { field ->
            Column(modifier = Modifier.width(width)) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall),
                    contentAlignment = Alignment.Center,
                    content = { field() },
                )
                Box(modifier = Modifier.fillMaxWidth().size(2.dp).background(underline))
            }
        },
    )
}

@Composable
private fun ItemLabel(label: String) {
    if (label.isBlank()) return
    Text(
        text = "$label)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Dimens.spacingTiny),
    )
}

private fun choiceState(
    option: String,
    selected: String?,
    answer: String,
    answerState: AnswerState,
): AnswerChipState =
    when {
        answerState is AnswerState.Unanswered -> {
            if (option == selected) AnswerChipState.SELECTED else AnswerChipState.UNSELECTED
        }

        option == answer -> AnswerChipState.CORRECT
        option == selected -> AnswerChipState.INCORRECT
        else -> AnswerChipState.UNSELECTED
    }
