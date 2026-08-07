package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconErrorContainer
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconSuccessContainer

enum class WordCardState { NEUTRAL, CORRECT, INCORRECT }

enum class WordCardSize { COMPACT, EXPANDED }

/** The target word / prompt — the one visually loud element on a training step. */
@Composable
fun WordCard(
    word: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    state: WordCardState = WordCardState.NEUTRAL,
    size: WordCardSize = WordCardSize.COMPACT,
) {
    val stateColor = when (state) {
        WordCardState.CORRECT -> LexiconSuccess
        WordCardState.INCORRECT -> LexiconError
        WordCardState.NEUTRAL -> MaterialTheme.colorScheme.onBackground
    }
    val stateBackground = when (state) {
        WordCardState.CORRECT -> LexiconSuccessContainer
        WordCardState.INCORRECT -> LexiconErrorContainer
        WordCardState.NEUTRAL -> Color.Transparent
    }
    val textStyle = if (size == WordCardSize.COMPACT) {
        MaterialTheme.typography.displaySmall
    } else {
        MaterialTheme.typography.displayMedium
    }

    Column(
        modifier = modifier
            .clip(LexiconShapes.medium)
            .background(stateBackground)
            .padding(Dimens.spacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = word, style = textStyle, color = stateColor)
        sublabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
        }
    }
}
