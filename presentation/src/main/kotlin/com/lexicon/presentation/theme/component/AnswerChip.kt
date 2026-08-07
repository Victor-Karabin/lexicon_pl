package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconErrorContainer
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconSuccessContainer

enum class AnswerChipState { UNSELECTED, SELECTED, CORRECT, INCORRECT }

enum class AnswerChipVariant { CHIP, ROW }

/**
 * A selectable answer option — chip (puzzle/letter tile) or full-width row (option list, e.g.
 * Image Test's six options). Min touch target is 48dp per DESIGN.md §9.
 */
@Composable
fun AnswerChip(
    label: String,
    modifier: Modifier = Modifier,
    state: AnswerChipState = AnswerChipState.UNSELECTED,
    variant: AnswerChipVariant = AnswerChipVariant.CHIP,
    onClick: (() -> Unit)? = null,
) {
    val background = when (state) {
        AnswerChipState.UNSELECTED -> MaterialTheme.colorScheme.surfaceContainerHigh
        AnswerChipState.SELECTED -> MaterialTheme.colorScheme.secondaryContainer
        AnswerChipState.CORRECT -> LexiconSuccessContainer
        AnswerChipState.INCORRECT -> LexiconErrorContainer
    }
    val foreground = when (state) {
        AnswerChipState.UNSELECTED -> MaterialTheme.colorScheme.onSurface
        AnswerChipState.SELECTED -> MaterialTheme.colorScheme.primary
        AnswerChipState.CORRECT -> LexiconSuccess
        AnswerChipState.INCORRECT -> LexiconError
    }
    val icon = when (state) {
        AnswerChipState.CORRECT -> "✓ "
        AnswerChipState.INCORRECT -> "✗ "
        else -> ""
    }
    val shape = if (variant == AnswerChipVariant.CHIP) LexiconShapes.small else LexiconShapes.medium
    val widthModifier = if (variant == AnswerChipVariant.ROW) Modifier.fillMaxWidth() else Modifier
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .then(widthModifier)
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(background)
            .then(clickModifier)
            .padding(
                horizontal = if (variant == AnswerChipVariant.CHIP) Dimens.spacingLarge else Dimens.spacingMedium,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (variant == AnswerChipVariant.ROW) Arrangement.Start else Arrangement.Center,
    ) {
        Text(
            text = icon + label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = foreground,
        )
    }
}
