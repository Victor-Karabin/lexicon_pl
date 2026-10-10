package com.lexicon.presentation.course

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconErrorContainer
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconSuccessContainer
import com.lexicon.presentation.theme.LexiconWarning
import com.lexicon.presentation.theme.component.AnswerChip
import com.lexicon.presentation.theme.component.AnswerChipState
import com.lexicon.presentation.theme.component.AnswerChipVariant

private val PlayIconSize = 20.dp
private val SeekBarTouchHeight = 32.dp
private val SeekTrackHeight = 4.dp
private val SeekThumbRadius = 6.dp
private const val SEEK_TRACK_ALPHA = 0.3f
private const val MILLIS_PER_SECOND = 1000
private const val SHORT_OPTION_LENGTH = 2
private const val MAX_OPTIONS_IN_ROW = 3
private const val MEDIUM_OPTION_LENGTH = 10
private const val MEDIUM_OPTIONS_PER_ROW = 3
private const val SECONDS_PER_MINUTE = 60
private val AnswerLabelWidth = 28.dp
private val InfoButtonSize = 32.dp
private val PositionBadgeSize = 32.dp
private val LetterCellSize = 36.dp
private val MatchIconSize = 32.dp

private val GapTextPadding = 16.dp
private val GapMinWidth = 96.dp
private val GapMaxWidth = 280.dp

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
fun AudioPlayerRow(
    state: TrackState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = state.durationMs
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (duration > 0) state.positionMs.toFloat() / duration else 0f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(end = Dimens.spacingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (state.isPlaying) R.string.exercise_pause else R.string.exercise_play),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "${clock((fraction * duration).toLong())} / ${clock(duration)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SeekBar(
            fraction = fraction.coerceIn(0f, 1f),
            enabled = duration > 0,
            onSeeking = { dragging = it },
            onSeekDone = {
                dragging = null
                onSeek((it * duration).toLong())
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SeekBar(
    fraction: Float,
    enabled: Boolean,
    onSeeking: (Float) -> Unit,
    onSeekDone: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = MaterialTheme.colorScheme.onSurface
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = SEEK_TRACK_ALPHA)
    var latest by remember { mutableStateOf(fraction) }
    Canvas(
        modifier = modifier
            .height(SeekBarTouchHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (enabled) {
                    setProgress {
                        onSeekDone(it.coerceIn(0f, 1f))
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { onSeekDone(seekFraction(it.x, size.width, SeekThumbRadius.toPx())) }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        latest = seekFraction(it.x, size.width, SeekThumbRadius.toPx())
                        onSeeking(latest)
                    },
                    onHorizontalDrag = { change, _ ->
                        latest = seekFraction(change.position.x, size.width, SeekThumbRadius.toPx())
                        onSeeking(latest)
                    },
                    onDragEnd = { onSeekDone(latest) },
                    onDragCancel = { onSeekDone(latest) },
                )
            },
    ) {
        val track = SeekTrackHeight.toPx()
        val inset = SeekThumbRadius.toPx()
        val y = size.height / 2
        val x = inset + (size.width - 2 * inset) * fraction
        drawLine(inactive, Offset(inset, y), Offset(size.width - inset, y), strokeWidth = track, cap = StrokeCap.Round)
        drawLine(active, Offset(inset, y), Offset(x, y), strokeWidth = track, cap = StrokeCap.Round)
        drawCircle(active, radius = SeekThumbRadius.toPx(), center = Offset(x, y))
    }
}

private fun seekFraction(
    x: Float,
    width: Int,
    inset: Float,
): Float = ((x - inset) / (width - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)

private fun clock(ms: Long): String {
    val seconds = ms / MILLIS_PER_SECOND
    return "${seconds / SECONDS_PER_MINUTE}:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
}

@Composable
fun MinimalPairRow(
    item: MinimalPairItem,
    selected: String?,
    answerState: AnswerState,
    onSelect: (String) -> Unit,
    info: String? = null,
    onSpeak: (() -> Unit)? = null,
    prompt: String? = null,
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val checked = answerState !is AnswerState.Unanswered
    val tools: @Composable () -> Unit = {
        if (checked && onSpeak != null) {
            IconButton(onClick = onSpeak, modifier = Modifier.size(InfoButtonSize)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.word_pronounce, item.answer),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (checked) {
            Box(modifier = Modifier.size(InfoButtonSize)) {
                if (info != null) InfoButton(onClick = { showInfo = !showInfo })
            }
        }
    }
    val columns = when {
        item.options.all { it.length <= SHORT_OPTION_LENGTH } -> item.options.size
        item.options.size <= MAX_OPTIONS_IN_ROW -> item.options.size
        item.options.all { it.length <= MEDIUM_OPTION_LENGTH } -> MEDIUM_OPTIONS_PER_ROW
        else -> 2
    }.coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny)) {
        ItemLabel(item.label)
        if (prompt != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = richText(prompt), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                tools()
            }
        }
        item.options.chunked(columns).forEachIndexed { index, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { option ->
                    AnswerChip(
                        label = option,
                        modifier = Modifier.weight(1f),
                        state = choiceState(
                            option = option,
                            selected = selected,
                            answer = item.answer,
                            answerState = answerState,
                        ),
                        onClick = { onSelect(option) }.takeIf { !checked },
                    )
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                if (prompt == null && index == 0) tools()
            }
        }
        if (showInfo && info != null) Explanation(info)
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
    almost: List<Boolean> = emptyList(),
    infos: List<String?> = emptyList(),
) {
    var openInfo by rememberSaveable { mutableStateOf<Int?>(null) }
    val checked = answerState !is AnswerState.Unanswered
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
        ) {
            item.speaker?.let { speaker ->
                Text(
                    text = "$speaker:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = Dimens.spacingTiny),
                )
            }
            var gap = 0
            item.prompt.split(GAP_MARKER).forEachIndexed { index, fragment ->
                if (index > 0) {
                    val at = gap++
                    InlineGap(
                        value = values.getOrElse(at) { "" },
                        expected = item.answers.getOrElse(at) { "" },
                        isCorrect = correctness.getOrNull(at),
                        isAlmost = almost.getOrElse(at) { false },
                        answerState = answerState,
                        onValueChanged = { onValueChanged(at, it) },
                    )
                    if (checked && infos.getOrNull(at) != null) {
                        InfoButton(onClick = { openInfo = if (openInfo == at) null else at })
                    }
                }
                fragment.split(' ').filter { it.isNotBlank() }.forEach { word ->
                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = Dimens.spacingTiny),
                    )
                }
            }
        }
        openInfo?.let { at -> infos.getOrNull(at)?.let { Explanation(it) } }
    }
}

@Composable
fun TranscribeRow(
    item: TranscribeItem,
    value: String,
    isCorrect: Boolean?,
    answerState: AnswerState,
    onValueChanged: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isAlmost: Boolean = false,
    info: String? = null,
    prompt: String? = null,
    labelWidth: Dp? = null,
    onSpeak: (() -> Unit)? = null,
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val checked = answerState !is AnswerState.Unanswered
    val inline = labelWidth != null || (prompt == null && item.label.length <= SHORT_OPTION_LENGTH)
    val indent = if (inline) (labelWidth ?: AnswerLabelWidth) + Dimens.spacingSmall else 0.dp
    val border = when {
        isCorrect == true -> LexiconSuccess
        isAlmost -> LexiconWarning
        isCorrect == false -> LexiconError
        else -> MaterialTheme.colorScheme.outline
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!inline) {
            Text(
                text = richText(
                    listOfNotNull(
                        item.label.takeIf {
                            it.isNotBlank()
                        }?.let { if (it.length <= SHORT_OPTION_LENGTH) "$it)" else it },
                        prompt,
                    ).joinToString("  "),
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = Dimens.spacingTiny),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            if (inline) {
                Text(
                    text = if (labelWidth == null) "${item.label})" else item.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(labelWidth ?: AnswerLabelWidth),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChanged,
                enabled = !checked,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, border, LexiconShapes.small)
                            .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) { field() }
                        if (isCorrect == true) Icon(Icons.Default.Check, contentDescription = null, tint = LexiconSuccess)
                    }
                },
            )
            if (checked && onSpeak != null) {
                IconButton(onClick = onSpeak, modifier = Modifier.size(InfoButtonSize)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.word_pronounce, item.answer),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (checked && (info != null || onSpeak == null)) {
                Box(modifier = Modifier.size(InfoButtonSize)) {
                    if (info != null) InfoButton(onClick = { showInfo = !showInfo })
                }
            }
        }
        if (isCorrect == false) {
            ExpectedAnswer(
                expected = item.answer,
                isAlmost = isAlmost,
                modifier = Modifier.padding(start = indent, top = Dimens.spacingTiny),
            )
        }
        if (showInfo && info != null) Explanation(info, modifier = Modifier.padding(start = indent))
    }
}

@Composable
fun OrderLineRow(
    speaker: String?,
    text: String,
    position: String,
    expected: String,
    isCorrect: Boolean?,
    onClick: (() -> Unit)?,
) {
    val border = when (isCorrect) {
        true -> LexiconSuccess
        false -> LexiconError
        null -> MaterialTheme.colorScheme.outlineVariant
    }
    val badge = when {
        isCorrect == true -> LexiconSuccessContainer
        isCorrect == false -> LexiconErrorContainer
        position.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val number = when (isCorrect) {
        true -> LexiconSuccess
        false -> LexiconError
        null -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LexiconShapes.small)
            .border(1.dp, border, LexiconShapes.small)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(Dimens.spacingSmall),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(PositionBadgeSize)
                .clip(CircleShape)
                .background(badge)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(position, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = number)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = richText(listOfNotNull(speaker?.let { "**$it:**" }, text).joinToString(" ")),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (isCorrect == false) ExpectedAnswer(expected = expected, isAlmost = false)
        }
    }
}

@Composable
fun InfoButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(InfoButtonSize)) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = stringResource(R.string.lesson_flow_explain),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun Explanation(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = richText(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = Dimens.spacingTiny),
    )
}

@Composable
private fun ExpectedAnswer(
    expected: String,
    isAlmost: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(if (isAlmost) R.string.lesson_flow_almost_expected else R.string.lesson_flow_expected, expected),
        style = MaterialTheme.typography.bodySmall,
        color = if (isAlmost) LexiconWarning else LexiconError,
        modifier = modifier,
    )
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
    isAlmost: Boolean,
    answerState: AnswerState,
    onValueChanged: (String) -> Unit,
) {
    val underline = when {
        isCorrect == true -> LexiconSuccess
        isAlmost -> LexiconWarning
        isCorrect == false -> LexiconError
        else -> MaterialTheme.colorScheme.outline
    }
    val style = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typedWidth = with(density) { measurer.measure(value, style).size.width.toDp() } + GapTextPadding
    val width = typedWidth.coerceIn(GapMinWidth, GapMaxWidth)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = value,
            onValueChange = onValueChanged,
            enabled = answerState is AnswerState.Unanswered,
            singleLine = true,
            modifier = Modifier.width(width),
            textStyle = style,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { field ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingTiny),
                        contentAlignment = Alignment.Center,
                        content = { field() },
                    )
                    Box(modifier = Modifier.fillMaxWidth().size(2.dp).background(underline))
                }
            },
        )
        if (isCorrect == false) {
            Text(
                text = expected,
                style = MaterialTheme.typography.labelMedium,
                color = if (isAlmost) LexiconWarning else LexiconError,
                textAlign = TextAlign.Center,
            )
        }
    }
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
