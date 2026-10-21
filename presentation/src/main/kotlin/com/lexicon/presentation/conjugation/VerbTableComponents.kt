package com.lexicon.presentation.conjugation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.course.TranscribeRow

private val PersonColumnWidth = 96.dp

internal val DrillImageHeight = 120.dp

internal const val SEARCH_SETTLE_MS = 300L

@Composable
fun VerbHeading(
    infinitive: String,
    translation: String?,
    modifier: Modifier = Modifier,
    details: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier = modifier) {
        Text(
            text = infinitive,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        translation?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        details()
    }
}

@Composable
fun PersonFormRow(
    person: String,
    typed: String,
    expected: String,
    verdict: AnswerVerdict?,
    onAnswerChanged: (String) -> Unit,
    onSpeak: () -> Unit,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    info: String? = null,
    placeholder: String? = null,
    focusRequester: FocusRequester? = null,
    labelWidth: Dp = PersonColumnWidth,
) {
    Box(modifier = modifier) {
        TranscribeRow(
            item = TranscribeItem(person, expected),
            value = typed,
            isCorrect = verdict?.let { it == AnswerVerdict.CORRECT },
            answerState = when (verdict) {
                null -> AnswerState.Unanswered
                AnswerVerdict.CORRECT -> AnswerState.Correct
                else -> AnswerState.Incorrect()
            },
            onValueChanged = onAnswerChanged,
            isAlmost = verdict == AnswerVerdict.ALMOST,
            info = info,
            placeholder = placeholder,
            focusRequester = focusRequester,
            labelWidth = labelWidth,
            onSpeak = onSpeak,
            imeAction = imeAction,
        )
    }
}
