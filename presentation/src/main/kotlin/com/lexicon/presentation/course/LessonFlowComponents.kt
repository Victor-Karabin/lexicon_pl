package com.lexicon.presentation.course

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.course.LessonResults
import com.lexicon.model.course.LessonPhrase
import com.lexicon.model.course.LessonTable
import com.lexicon.model.course.Transcript
import com.lexicon.model.course.WritingReview
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes

private val TableCellWidth = 120.dp

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
fun PhraseList(
    phrases: List<LessonPhrase>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        phrases.forEachIndexed { index, phrase ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall)) {
                Text(phrase.polish, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    phrase.english,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            content()
        }
    }
}

@Composable
fun TranscriptCard(
    transcript: Transcript,
    isOpen: Boolean,
    onToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (onToggle != null) {
            TextButton(onClick = onToggle) {
                Text(stringResource(if (isOpen) R.string.lesson_flow_hide_transcript else R.string.lesson_flow_show_transcript))
            }
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

@Composable
fun HintCard(
    hint: String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(onClick = { open = !open }) {
            Text(stringResource(if (open) R.string.lesson_flow_hide_hint else R.string.lesson_flow_show_hint))
        }
        if (open) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Text(
                    text = richText(hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                )
            }
        }
    }
}

@Composable
fun FreeWritingFields(
    fields: List<String>,
    values: List<String>,
    enabled: Boolean,
    onValueChanged: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
        fields.forEachIndexed { index, label ->
            OutlinedTextField(
                value = values.getOrElse(index) { "" },
                onValueChange = { onValueChanged(index, it) },
                label = { Text(label) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrectEnabled = false),
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ReviewCard(
    review: WritingReview,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = null, modifier = modifier) {
        if (review.strengths.isNotEmpty()) {
            Text(stringResource(R.string.lesson_flow_review_strengths), style = MaterialTheme.typography.titleSmall)
            review.strengths.forEach { Text(richText("• $it"), style = MaterialTheme.typography.bodyMedium) }
        }
        if (review.improvements.isNotEmpty()) {
            Text(
                stringResource(R.string.lesson_flow_review_improvements),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
            review.improvements.forEach { Text(richText("• $it"), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun ModelAnswerCard(
    model: List<String>,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = stringResource(R.string.lesson_flow_model_answer), modifier = modifier) {
        model.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun LessonResultsView(
    results: LessonResults,
    title: String?,
    modifier: Modifier = Modifier,
) {
    val overall = results.overall
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
        Text(
            stringResource(R.string.lesson_flow_completed),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        title?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(
            stringResource(R.string.lesson_flow_results, overall.correct, overall.total, overall.percent),
            style = MaterialTheme.typography.titleMedium,
        )
        SectionCard(title = null) {
            results.steps.forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall)) {
                    Text(step.step.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.lesson_flow_step_result, step.score.correct, step.score.total),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
