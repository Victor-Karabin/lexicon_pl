package com.lexicon.presentation.course

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.StepScore
import com.lexicon.model.course.LessonTable
import com.lexicon.model.course.Transcript
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconWarning

private val TableCellWidth = 120.dp

private const val POLISH_LETTERS = "ąćęłńóśźż"

private val emphasis = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")

fun richText(text: String): AnnotatedString =
    buildAnnotatedString {
        var at = 0
        emphasis.findAll(text).forEach { match ->
            append(text.substring(at, match.range.first))
            val bold = match.groupValues[1]
            if (bold.isNotEmpty()) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            } else {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(match.groupValues[2]) }
            }
            at = match.range.last + 1
        }
        append(text.substring(at))
    }

@Composable
fun LessonTableView(
    table: LessonTable,
    modifier: Modifier = Modifier,
) {
    val hasHeader = table.header.any { it.isNotBlank() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, LexiconShapes.small)
            .padding(Dimens.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        if (hasHeader) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                table.header.forEach { cell ->
                    Text(
                        text = richText(cell),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(TableCellWidth),
                    )
                }
            }
        }
        table.rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                row.forEach { cell ->
                    Text(
                        text = richText(cell),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(TableCellWidth),
                    )
                }
            }
        }
    }
}

@Composable
fun TranscriptCard(
    transcript: Transcript,
    isUnlocked: Boolean,
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!isUnlocked) {
            Text(
                text = stringResource(R.string.lesson_flow_transcript_locked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        TextButton(onClick = onToggle) {
            Text(stringResource(if (isOpen) R.string.lesson_flow_hide_transcript else R.string.lesson_flow_show_transcript))
        }
        if (!isOpen) return
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                transcript.sections.forEach { section ->
                    section.title?.let {
                        Text(text = it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    section.lines.forEach { line ->
                        Text(
                            text = buildAnnotatedString {
                                line.speaker?.let { speaker ->
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("$speaker: ") }
                                }
                                append(richText(line.text))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PolishLettersBar(
    onLetter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
    ) {
        POLISH_LETTERS.forEach { letter ->
            SuggestionChip(onClick = { onLetter(letter.toString()) }, label = { Text(letter.toString()) })
        }
    }
}

@Composable
fun ItemFeedback(
    label: String,
    verdict: AnswerVerdict?,
    expected: String,
    feedback: String?,
    modifier: Modifier = Modifier,
) {
    if (verdict == null) return
    val wrong = verdict != AnswerVerdict.CORRECT
    if (!wrong && feedback == null) return
    Column(modifier = modifier.fillMaxWidth().padding(start = Dimens.spacingLarge, bottom = Dimens.spacingSmall)) {
        if (wrong) {
            Text(
                text = "$label) " + if (verdict == AnswerVerdict.ALMOST) {
                    stringResource(R.string.lesson_flow_almost_expected, expected)
                } else {
                    stringResource(R.string.lesson_flow_expected, expected)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (verdict == AnswerVerdict.ALMOST) LexiconWarning else LexiconError,
            )
        }
        feedback?.let {
            Text(
                text = richText(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FreeWritingFields(
    fields: List<String>,
    values: List<String>,
    onValueChanged: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
        fields.forEachIndexed { index, label ->
            val value = values.getOrElse(index) { "" }
            val text = rememberEditableText(value)
            val letters = rememberLetterReceiver(text) { onValueChanged(index, it) }
            OutlinedTextField(
                value = text.value,
                onValueChange = {
                    text.value = it
                    if (it.text != value) onValueChanged(index, it.text)
                },
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                minLines = 4,
                modifier = Modifier.fillMaxWidth().then(letters),
            )
        }
    }
}

@Composable
fun SelfCheckCard(
    model: List<String>,
    checklist: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Text(stringResource(R.string.lesson_flow_model_answer), style = MaterialTheme.typography.titleSmall)
            model.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
            Text(
                stringResource(R.string.lesson_flow_checklist),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
            checklist.forEach { Text(richText("• $it"), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun StepScoreCard(
    score: StepScore,
    modifier: Modifier = Modifier,
) {
    val color: Color = if (score.passed) LexiconSuccess else LexiconError
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium)) {
            Text(
                text = stringResource(R.string.lesson_flow_step_score, score.step.title, score.percent, score.correct, score.total),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
            Text(
                text = if (score.passed) {
                    stringResource(R.string.lesson_flow_step_passed)
                } else {
                    stringResource(R.string.lesson_flow_step_failed, (score.passMark * 100).toInt())
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
        }
    }
}
