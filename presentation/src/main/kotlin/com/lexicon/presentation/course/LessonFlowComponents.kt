package com.lexicon.presentation.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.course.LessonResults
import com.lexicon.model.course.GrammarRule
import com.lexicon.model.course.ItemPicture
import com.lexicon.model.course.LessonPhrase
import com.lexicon.model.course.LessonTable
import com.lexicon.model.course.Transcript
import com.lexicon.model.course.WritingReview
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabulary.statusOf
import com.lexicon.presentation.R
import com.lexicon.presentation.presets.WordRow
import com.lexicon.presentation.presets.detailLine
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes

private val TableCellWidth = 120.dp
private const val MAX_EXERCISES_TO_IMPROVE = 5

private val emphasis = Regex("""\*\*(.+?)\*\*|\*(.+?)\*|~~(.+?)~~""")
private val SwatchSize = 32.dp
private val PictureIconSize = 28.dp
private const val EMOJI_SCALE = 1.4f

private val pictureIcons = mapOf(
    "table" to Icons.Default.TableRestaurant,
    "board" to Icons.Default.CoPresent,
)

fun richText(text: String): AnnotatedString =
    buildAnnotatedString {
        var at = 0
        emphasis.findAll(text).forEach { match ->
            append(text.substring(at, match.range.first))
            val (bold, italic, struck) = match.destructured
            when {
                bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
                else -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(struck) }
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
fun ItemPictureView(
    picture: ItemPicture,
    modifier: Modifier = Modifier,
) {
    when (picture) {
        is ItemPicture.Swatch ->
            Box(
                modifier = modifier
                    .size(SwatchSize)
                    .background(Color(picture.color), LexiconShapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.outline, LexiconShapes.small),
            )

        is ItemPicture.Symbol ->
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = picture.icon?.let(pictureIcons::get)
                val emoji = picture.emoji
                when {
                    emoji != null ->
                        Text(
                            emoji,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = MaterialTheme.typography.titleLarge.fontSize * EMOJI_SCALE,
                            ),
                        )
                    icon != null ->
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(PictureIconSize),
                        )
                }
                Text(picture.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
    }
}

@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Dimens.spacingTiny),
    )
}

@Composable
fun RuleView(
    rule: GrammarRule,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        if (showTitle) {
            Text(
                richText(stringResource(R.string.lesson_flow_rule_heading, rule.number, rule.title)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        rule.tables.forEach { LessonTableView(it) }
        rule.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun WordBoxCard(
    words: List<String>,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = null, modifier = modifier) {
        Text(words.joinToString("  ·  "), style = MaterialTheme.typography.bodyLarge)
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
                Text(richText(phrase.polish), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    richText(phrase.english),
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
            TextLink(stringResource(if (isOpen) R.string.lesson_flow_hide_transcript else R.string.lesson_flow_show_transcript), onToggle)
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
    RevealCard(
        showLabel = stringResource(R.string.lesson_flow_show_hint),
        hideLabel = stringResource(R.string.lesson_flow_hide_hint),
        modifier = modifier,
    ) {
        Text(text = richText(hint), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun NewWordsCard(
    words: List<LessonPhrase>,
    entries: Map<String, Word>,
    statuses: Map<VocabularyId, WordStatus>,
    onPronounce: (String) -> Unit,
    onStatusCycled: (VocabularyId) -> Unit,
    modifier: Modifier = Modifier,
) {
    RevealCard(
        showLabel = stringResource(R.string.lesson_flow_new_words, words.size),
        hideLabel = stringResource(R.string.lesson_flow_hide_new_words),
        modifier = modifier,
        padded = false,
    ) {
        words.forEachIndexed { index, phrase ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val word = entries[phrase.polish]
            WordRow(
                word = word,
                text = phrase.polish,
                translation = phrase.english,
                status = word?.let { statuses.statusOf(it.id) },
                onStatusCycled = { word?.let { onStatusCycled(it.id) } },
                onPronounce = { onPronounce(phrase.polish.trimEnd('…')) },
                detail = word?.detailLine() ?: phrase.detailLine(),
            )
        }
    }
}

@Composable
private fun RevealCard(
    showLabel: String,
    hideLabel: String,
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        TextLink(if (open) hideLabel else showLabel) { open = !open }
        if (open) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.fillMaxWidth().then(if (padded) Modifier.padding(Dimens.spacingMedium) else Modifier)) {
                    content()
                }
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
    multiline: List<Boolean> = emptyList(),
) {
    val focusManager = LocalFocusManager.current
    val answerDone = LocalAnswerDone.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
        fields.forEachIndexed { index, label ->
            val isMultiline = multiline.getOrElse(index) { false }
            OutlinedTextField(
                value = values.getOrElse(index) { "" },
                onValueChange = { onValueChanged(index, it) },
                label = { Text(label) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = false,
                    imeAction = when {
                        isMultiline -> ImeAction.Default
                        index == fields.lastIndex -> ImeAction.Done
                        else -> ImeAction.Next
                    },
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        answerDone()
                    },
                ),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonResultsView(
    results: LessonResults,
    title: String?,
    onRuleOpened: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val overall = results.overall
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
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
        SectionCard(title = stringResource(R.string.lesson_flow_to_improve)) {
            if (results.toImprove.isEmpty() && results.overall.correct == results.overall.total) {
                Text(stringResource(R.string.lesson_flow_no_mistakes), style = MaterialTheme.typography.bodyMedium)
            }
            results.toImprove.take(MAX_EXERCISES_TO_IMPROVE).forEach { exercise ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.lesson_flow_exercise, exercise.screen.id, exercise.screen.title),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.lesson_flow_first_try, exercise.firstTry.correct, exercise.firstTry.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (results.missedPolishLetters > 0) {
                Text(
                    pluralStringResource(R.plurals.lesson_flow_polish_letters, results.missedPolishLetters, results.missedPolishLetters),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (results.rulesToRevisit.isNotEmpty()) {
                Text(stringResource(R.string.lesson_flow_rules_to_revisit), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
                ) {
                    results.rulesToRevisit.forEach { rule -> RuleChip(rule, onClick = { onRuleOpened(rule.number) }) }
                }
            }
        }
        SectionCard(title = stringResource(R.string.lesson_flow_by_step)) {
            results.steps.forEach { step ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(step.step.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.lesson_flow_step_result, step.score.correct, step.score.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
