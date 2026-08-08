package com.lexicon.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
