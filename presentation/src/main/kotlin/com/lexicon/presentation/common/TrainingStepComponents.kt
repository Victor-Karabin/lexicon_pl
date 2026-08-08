package com.lexicon.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.component.AnswerChip
import com.lexicon.presentation.theme.component.AnswerChipState
import com.lexicon.presentation.theme.component.AnswerChipVariant

/**
 * The pieces a training step is assembled from. They live here rather than in each screen because
 * Mix re-renders the same steps inside its own session: a copy per screen drifts, and the same
 * exercise then looks different depending on how it was reached.
 */

private val ClueImageHeight = 180.dp

/** Tall enough that the answer pair reads as the step's primary action, without dominating it. */
private val AnswerButtonHeight = 88.dp

/** The one place answer outcomes turn into colour, so every screen reads the same. */
@Composable
fun answerStateColor(answerState: AnswerState): Color =
    when (answerState) {
        is AnswerState.Correct -> LexiconSuccess
        is AnswerState.Incorrect, is AnswerState.Skipped -> LexiconError
        is AnswerState.Unanswered -> MaterialTheme.colorScheme.outline
    }

/**
 * The answer assembled from letter tiles. Blank text keeps the box at its natural height, so the
 * layout doesn't jump as the first tile is placed.
 */
@Composable
fun BuiltAnswerField(
    answer: String,
    answerState: AnswerState,
    modifier: Modifier = Modifier,
) {
    val answerColor = answerStateColor(answerState)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LexiconShapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, answerColor, LexiconShapes.small)
            .padding(Dimens.spacingMedium),
    ) {
        Text(
            text = answer.ifEmpty { " " },
            style = MaterialTheme.typography.headlineSmall,
            color = answerColor,
        )
    }
}

/**
 * The image a step is asked about. [fallbackText] stands in whenever there is no image or it fails
 * to load, so a step stays answerable instead of showing an empty frame (Puzzle spec: fall back to
 * the base text).
 */
@Composable
fun ClueImage(
    imageUrl: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().height(ClueImageHeight)) {
        if (imageUrl == null) {
            Text(fallbackText, style = MaterialTheme.typography.headlineSmall)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                error = { Text(fallbackText, style = MaterialTheme.typography.headlineSmall) },
            )
        }
    }
}

/**
 * The single-select answer options of a multiple-choice step. A non-null [correctOption] means the
 * step has been checked, which is what turns the options from selectable into graded.
 */
@Composable
fun AnswerOptionList(
    options: List<String>,
    selectedOption: String?,
    correctOption: String?,
    enabled: Boolean,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        options.forEach { option ->
            val state = when {
                correctOption == option -> AnswerChipState.CORRECT
                correctOption != null && selectedOption == option -> AnswerChipState.INCORRECT
                selectedOption == option -> AnswerChipState.SELECTED
                else -> AnswerChipState.UNSELECTED
            }
            AnswerChip(
                label = option,
                state = state,
                variant = AnswerChipVariant.ROW,
                onClick = { onOptionSelected(option) }.takeIf { enabled },
                modifier = Modifier.padding(vertical = Dimens.spacingTiny),
            )
        }
    }
}

/** How an answer button is coloured: by what it means, not by a raw colour. */
enum class AnswerTone {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
    ;

    companion object {
        /** null — no outcome yet — leaves the button neutral. */
        fun of(isCorrect: Boolean?): AnswerTone =
            when (isCorrect) {
                true -> POSITIVE
                false -> NEGATIVE
                null -> NEUTRAL
            }
    }
}

/**
 * True or False's answer pair. It belongs in a screen's bottom slot rather than inline with the
 * word: these buttons are the step's primary action, and the standalone training places them within
 * thumb reach, so a Mix step has to sit there too or the same exercise moves around.
 */
@Composable
fun TrueOrFalseAnswerRow(
    trueTone: AnswerTone,
    falseTone: AnswerTone,
    enabled: Boolean,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(AnswerButtonHeight),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        AnswerToneButton(
            label = stringResource(R.string.action_true),
            tone = trueTone,
            enabled = enabled,
            onClick = { onAnswer(true) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        AnswerToneButton(
            label = stringResource(R.string.action_false),
            tone = falseTone,
            enabled = enabled,
            onClick = { onAnswer(false) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

/**
 * The disabled colours repeat the enabled ones: the button is disabled the moment it is answered,
 * and Material would otherwise grey the container out exactly when its colour starts carrying the
 * result.
 */
@Composable
private fun AnswerToneButton(
    label: String,
    tone: AnswerTone,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = when (tone) {
        AnswerTone.POSITIVE -> LexiconSuccess
        AnswerTone.NEGATIVE -> LexiconError
        AnswerTone.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when (tone) {
        AnswerTone.NEUTRAL -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> Color.White
    }
    Button(
        onClick = debounced(onClick = onClick),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
        modifier = modifier,
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}

/** Renders nothing until the step is answered, which is when there is a status worth naming. */
@Composable
fun AnswerStatusLabel(
    answerState: AnswerState,
    modifier: Modifier = Modifier,
) {
    val label = when (answerState) {
        is AnswerState.Correct -> stringResource(R.string.status_correct)
        is AnswerState.Incorrect -> stringResource(R.string.status_incorrect)
        is AnswerState.Skipped -> stringResource(R.string.status_skipped)
        is AnswerState.Unanswered -> null
    } ?: return

    Text(
        text = label,
        color = answerStateColor(answerState),
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}
