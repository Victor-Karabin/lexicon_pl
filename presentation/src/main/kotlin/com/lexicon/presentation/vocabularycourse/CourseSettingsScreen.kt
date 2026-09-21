package com.lexicon.presentation.vocabularycourse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.vocabularycourse.CourseSettings
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.main.courseTrainings
import com.lexicon.presentation.main.trainingDisplayName
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionText
import com.lexicon.presentation.theme.component.TileSkin
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private val QueueMedallionSize = 32.dp
private val SliderThumbSize = 20.dp
private val SliderTrackHeight = 4.dp
private val MoveIconSize = 20.dp

@Composable
fun CourseSettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val done = { viewModel.onDone(onClose) }

    BackHandler(onBack = done)

    CourseSettingsContent(
        uiState = uiState,
        onClose = done,
        onNewWordsChanged = viewModel::onNewWordsChanged,
        onReviewsChanged = viewModel::onReviewsChanged,
        onTrainingAdded = viewModel::onTrainingAdded,
        onTurnRemoved = viewModel::onTurnRemoved,
        onMove = viewModel::onMove,
        modifier = modifier,
    )
}

@Composable
private fun CourseSettingsContent(
    uiState: CourseSettingsUiState,
    onClose: () -> Unit,
    onNewWordsChanged: (Int) -> Unit,
    onReviewsChanged: (Int) -> Unit,
    onTrainingAdded: (String) -> Unit,
    onTurnRemoved: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrainingTopBar(title = stringResource(R.string.vocabulary_course_settings_title), onClose = onClose)
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            val skin = tileSkin()

            Text(
                text = stringResource(R.string.vocabulary_course_settings_scope),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AmountSlider(
                label = stringResource(R.string.vocabulary_course_new_words),
                value = uiState.newWordsADay,
                range = MIN_NEW_WORDS_A_DAY..CourseSettings.MAX_WORDS_A_DAY,
                onChange = onNewWordsChanged,
            )
            AmountSlider(
                label = stringResource(R.string.vocabulary_course_reviews),
                value = uiState.reviewsADay,
                range = 0..CourseSettings.MAX_WORDS_A_DAY,
                onChange = onReviewsChanged,
            )

            SectionLabel(stringResource(R.string.vocabulary_course_trainings))
            TrainingPicker(onAdd = onTrainingAdded)

            SectionLabel(stringResource(R.string.vocabulary_course_queue))
            Text(
                text = stringResource(
                    if (uiState.keptLastTraining) R.string.vocabulary_course_queue_keeps_one else R.string.vocabulary_course_queue_note,
                    uiState.queue.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            QueueList(
                queue = uiState.queue,
                skin = skin,
                onMove = onMove,
                onRemove = onTurnRemoved,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = value.coerceIn(range).toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(SliderThumbSize)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    modifier = Modifier.height(SliderTrackHeight),
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null,
                )
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingPicker(onAdd: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        courseTrainings.forEach { entry ->
            AssistChip(
                onClick = { onAdd(entry.id) },
                label = { Text(entry.displayName) },
                leadingIcon = {
                    Icon(imageVector = entry.icon, contentDescription = null, modifier = Modifier.size(MoveIconSize))
                },
            )
        }
    }
}

@Composable
private fun QueueList(
    queue: ImmutableList<String>,
    skin: TileSkin,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var draggedFrom by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }

    val gap = with(LocalDensity.current) { Dimens.spacingSmall.toPx() }
    val step = rowHeight + gap

    val landing = draggedFrom?.let { from -> landingFor(from, dragOffset, step, queue.lastIndex) }

    val from = draggedFrom

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        queue.forEachIndexed { index, training ->
            val isDragged = from == index

            val shift = when {
                from == null || landing == null || isDragged -> 0f
                landing > from && index in (from + 1)..landing -> -step
                landing < from && index in landing until from -> step
                else -> 0f
            }

            QueueRow(
                position = index + 1,
                training = training,
                skin = skin,
                isDragged = isDragged,
                onMoveEarlier = { onMove(index, index - 1) },
                onMoveLater = { onMove(index, index + 1) },
                onRemove = { onRemove(index) },
                modifier = Modifier
                    .onSizeChanged { rowHeight = it.height }
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) dragOffset else shift }
                    .pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedFrom = index
                                dragOffset = 0f
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                dragOffset += drag.y
                            },
                            onDragEnd = {
                                val to = landingFor(index, dragOffset, rowHeight + gap, queue.lastIndex)
                                if (to != index) onMove(index, to)
                                draggedFrom = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedFrom = null
                                dragOffset = 0f
                            },
                        )
                    },
            )
        }
    }
}

private fun landingFor(
    from: Int,
    offset: Float,
    step: Float,
    lastIndex: Int,
): Int = if (step <= 0f) from else (from + (offset / step).roundToInt()).coerceIn(0, lastIndex)

@Composable
private fun QueueRow(
    position: Int,
    training: String,
    skin: TileSkin,
    isDragged: Boolean,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moveEarlier = stringResource(R.string.vocabulary_course_move_earlier)
    val moveLater = stringResource(R.string.vocabulary_course_move_later)

    GradientTile(
        skin = skin,
        padding = Dimens.spacingSmall,
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(moveEarlier) {
                    onMoveEarlier()
                    true
                },
                CustomAccessibilityAction(moveLater) {
                    onMoveLater()
                    true
                },
            )
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.vocabulary_course_reorder),
                tint = if (isDragged) skin.onTile else skin.muted(),
            )
            Medallion(skin = skin, size = QueueMedallionSize) { MedallionText("$position", skin) }
            Text(
                text = trainingDisplayName(training),
                style = MaterialTheme.typography.bodyLarge,
                color = skin.onTile,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.vocabulary_course_remove_turn),
                    tint = skin.onTile,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@LightDarkPreview
@Composable
private fun CourseSettingsPreview() {
    LexiconTheme {
        CourseSettingsContent(
            uiState = CourseSettingsUiState(
                isLoading = false,
                queue = persistentListOf("word_match", "dictation", "word_match", "true_or_false"),
            ),
            onClose = {},
            onNewWordsChanged = {},
            onReviewsChanged = {},
            onTrainingAdded = {},
            onTurnRemoved = {},
            onMove = { _, _ -> },
        )
    }
}
