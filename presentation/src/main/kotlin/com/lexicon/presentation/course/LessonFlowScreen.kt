package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.GAP_MARKER
import com.lexicon.interactors.course.GapFillItem
import com.lexicon.interactors.course.LessonSession
import com.lexicon.interactors.course.MinimalPairItem
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.GAP_PATTERN
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonTrack
import com.lexicon.model.course.Transcript
import com.lexicon.model.course.allTracks
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

private val ProgressSize = 18.dp
private val TableLabelWidth = 120.dp
private val TableLabelLength = 3..16

class LessonFlowActions(
    val onClose: () -> Unit,
    val onFinished: () -> Unit,
    val onNextLesson: (LessonId) -> Unit,
    val onPlay: (LessonTrack) -> Unit,
    val onSeek: (LessonTrack, Long) -> Unit,
    val onAnswerChanged: (String, String) -> Unit,
    val onLineTapped: (String) -> Unit,
    val onCheck: () -> Unit,
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onTranscriptToggled: (String) -> Unit,
    val onSpeak: (String) -> Unit,
)

@Composable
fun LessonFlowScreen(
    onClose: () -> Unit,
    onFinished: () -> Unit,
    onNextLesson: (LessonId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonFlowViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonFlowContent(
        uiState = uiState,
        actions = LessonFlowActions(
            onClose = onClose,
            onFinished = onFinished,
            onNextLesson = onNextLesson,
            onPlay = viewModel::onPlay,
            onSeek = viewModel::onSeek,
            onAnswerChanged = viewModel::onAnswerChanged,
            onLineTapped = viewModel::onLineTapped,
            onCheck = viewModel::onCheck,
            onNext = viewModel::onNext,
            onBack = viewModel::onBack,
            onTranscriptToggled = viewModel::onTranscriptToggled,
            onSpeak = viewModel::onSpeak,
        ),
        modifier = modifier,
    )
}

@Composable
private fun LessonFlowContent(
    uiState: LessonFlowUiState,
    actions: LessonFlowActions,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is LessonFlowUiState.Loaded -> {
            val session = uiState.session
            when {
                uiState.results != null && uiState.lessonNumber != null -> stringResource(
                    R.string.course_lesson_number,
                    uiState.lessonNumber,
                )
                uiState.results != null -> stringResource(R.string.course_title)
                else -> stringResource(R.string.lesson_flow_position, session.screen.id, session.position, session.total)
            }
        }
        else -> stringResource(R.string.course_title)
    }

    AnswerInputs {
        Scaffold(
            modifier = modifier,
            topBar = { TrainingTopBar(title = title, onClose = actions.onClose) },
            bottomBar = { if (uiState is LessonFlowUiState.Loaded) Footer(uiState, actions) },
        ) { padding ->
            when (uiState) {
                LessonFlowUiState.Loading ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                LessonFlowUiState.NotFound ->
                    Box(Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.lesson_not_found), style = MaterialTheme.typography.bodyMedium)
                    }

                is LessonFlowUiState.Loaded ->
                    key(uiState.session.index, uiState.results != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                                .padding(Dimens.spacingMedium),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                        ) {
                            val results = uiState.results
                            if (results != null) LessonResultsView(results, uiState.lessonTitle) else ScreenBody(uiState, actions)
                        }
                    }
            }
        }
    }
}

@Composable
private fun ScreenBody(
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    val screen = session.screen

    Text(screen.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text(richText(screen.instruction), style = MaterialTheme.typography.bodyMedium)
    screen.tracks.forEach { TrackPlayer(it, uiState, actions) }
    screen.hint?.let { HintCard(it) }

    when (screen) {
        is LessonScreen.Reference -> ReferenceBody(screen, uiState, actions)
        is LessonScreen.Write -> WriteBody(screen, session, actions)
        is LessonScreen.Choice -> ChoiceBody(screen, session, actions)
        is LessonScreen.GapFill -> GapFillBody(screen, uiState, actions)
        is LessonScreen.FreeWriting -> FreeWritingBody(screen, uiState, actions)
        is LessonScreen.Ordering -> OrderingBody(screen, session, actions)
        is LessonScreen.Form -> FormBody(screen, session, actions)
    }

    val transcript = screen.transcript
    if (transcript != null && screen.allTracks().isEmpty() && session.isTranscriptUnlocked(screen)) {
        TranscriptCard(
            transcript = transcript,
            isOpen = transcript.isOpen || screen.id in uiState.openTranscripts,
            onToggle = { actions.onTranscriptToggled(screen.id) }.takeUnless { transcript.isOpen },
        )
    }

    if (session.isChecked) {
        val score = session.screenScore(screen)
        Text(stringResource(R.string.exercise_score, score.correct, score.total), style = MaterialTheme.typography.titleMedium)
    }
}

private fun LessonScreen.transcriptFor(track: LessonTrack): Transcript? {
    val transcript = transcript ?: return null
    val tracks = allTracks()
    val index = tracks.indexOf(track)
    return when {
        index < 0 -> null
        transcript.sections.size == tracks.size ->
            transcript.copy(sections = persistentListOf(transcript.sections[index].copy(title = null)))
        index == 0 -> transcript
        else -> null
    }
}

@Composable
private fun TrackPlayer(
    track: LessonTrack,
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
    showLabel: Boolean = true,
) {
    val session = uiState.session
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny)) {
        if (showLabel) track.label?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
        val state = uiState.tracks.of(track.file)
        AudioPlayerRow(
            state = state,
            onPlayPause = { actions.onPlay(track) },
            onSeek = { actions.onSeek(track, it) },
        )
        if (state.isMissing) {
            Text(
                stringResource(R.string.exercise_audio_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val transcript = session.screen.transcriptFor(track)
        if (transcript != null && session.isTranscriptUnlocked(session.screen)) {
            TranscriptCard(
                transcript = transcript,
                isOpen = track.file in uiState.openTranscripts,
                onToggle = { actions.onTranscriptToggled(track.file) },
            )
        }
    }
}

@Composable
private fun ReferenceBody(
    screen: LessonScreen.Reference,
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    screen.tables.forEach { LessonTableView(it) }
    screen.groups.forEach { group ->
        SectionCard(title = group.title) {
            group.track?.let { TrackPlayer(it, uiState, actions, showLabel = false) }
            PhraseList(group.phrases)
        }
    }
    screen.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun WriteBody(
    screen: LessonScreen.Write,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    val labelWidth = TableLabelWidth.takeIf { screen.questions.all { it.prompt == null && it.label.length in TableLabelLength } }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        screen.questions.forEach { question ->
            QuestionField(screen.id, question, session, actions, screen.keyboard, labelWidth)
        }
    }
    screen.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun FormBody(
    screen: LessonScreen.Form,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    screen.groups.forEach { group ->
        SectionCard(title = group.title) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                group.fields.forEach { question -> QuestionField(screen.id, question, session, actions) }
            }
        }
    }
}

@Composable
private fun QuestionField(
    screenId: String,
    question: LessonQuestion,
    session: LessonSession,
    actions: LessonFlowActions,
    keyboard: AnswerKeyboard = AnswerKeyboard.TEXT,
    labelWidth: Dp? = null,
) {
    val verdict = session.verdict(screenId, question.key)
    TranscribeRow(
        item = TranscribeItem(question.label, session.expectedAnswer(screenId, question.key)),
        value = session.answer(screenId, question.key),
        isCorrect = verdict?.let { it == AnswerVerdict.CORRECT },
        answerState = answerState(verdict, session.isChecked(screenId)),
        onValueChanged = { actions.onAnswerChanged(question.key, it) },
        keyboardType = if (keyboard == AnswerKeyboard.DIGITS) KeyboardType.Number else KeyboardType.Text,
        isAlmost = verdict == AnswerVerdict.ALMOST,
        info = question.feedback,
        prompt = question.prompt,
        labelWidth = labelWidth,
    )
}

@Composable
private fun OrderingBody(
    screen: LessonScreen.Ordering,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        screen.lines.forEach { line ->
            OrderLineRow(
                speaker = line.speaker,
                text = line.text,
                position = session.answer(screen.id, line.key),
                expected = session.expectedAnswer(screen.id, line.key),
                isCorrect = session.verdict(screen.id, line.key)?.let { it == AnswerVerdict.CORRECT },
                onClick = { actions.onLineTapped(line.key) }.takeUnless { session.isChecked },
            )
        }
    }
    screen.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun ChoiceBody(
    screen: LessonScreen.Choice,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    if (screen.legend.isNotEmpty()) {
        SectionCard(title = null) {
            screen.legend.forEach { Text(richText(it), style = MaterialTheme.typography.bodyLarge) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
        screen.items.forEach { item ->
            val question = item.question
            MinimalPairRow(
                item = MinimalPairItem("", item.options, session.expectedAnswer(screen.id, question.key)),
                selected = session.answer(screen.id, question.key).ifEmpty { null },
                answerState = answerState(session.verdict(screen.id, question.key), session.isChecked),
                onSelect = { actions.onAnswerChanged(question.key, it) },
                info = question.feedback,
                onSpeak = { actions.onSpeak(question.answers.first()) }.takeIf { screen.legend.isEmpty() },
                prompt = question.prompt,
            )
        }
    }
}

@Composable
private fun GapFillBody(
    screen: LessonScreen.GapFill,
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    val questions = screen.questions.associateBy { it.key }
    screen.sections.forEach { section ->
        SectionCard(title = section.title) {
            section.track?.let { TrackPlayer(it, uiState, actions, showLabel = false) }
            section.lines.forEach { line ->
                val keys = GAP_PATTERN.findAll(line.text).map { it.groupValues[1] }.toList()
                val verdicts = keys.map { session.verdict(screen.id, it) }
                GapFillRow(
                    item = GapFillItem(
                        prompt = line.text.replace(GAP_PATTERN, GAP_MARKER),
                        answers = keys.map { session.expectedAnswer(screen.id, it) }.toImmutableList(),
                        speaker = line.speaker,
                    ),
                    values = keys.map { session.answer(screen.id, it) },
                    correctness = if (session.isChecked) verdicts.map { it == AnswerVerdict.CORRECT } else emptyList(),
                    almost = verdicts.map { it == AnswerVerdict.ALMOST },
                    infos = keys.map { questions[it]?.feedback },
                    answerState = when {
                        !session.isChecked -> AnswerState.Unanswered
                        verdicts.all { it == AnswerVerdict.CORRECT } -> AnswerState.Correct
                        else -> AnswerState.Incorrect()
                    },
                    onValueChanged = { at, value -> actions.onAnswerChanged(keys[at], value) },
                )
            }
        }
    }
    screen.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun FreeWritingBody(
    screen: LessonScreen.FreeWriting,
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    FreeWritingFields(
        fields = screen.fields,
        values = screen.fields.indices.map { session.answer(screen.id, it.toString()) },
        enabled = !session.isFinished && !uiState.isReviewing,
        onValueChanged = { index, value -> actions.onAnswerChanged(index.toString(), value) },
    )
    if (uiState.isReviewing) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            CircularProgressIndicator(modifier = Modifier.size(ProgressSize), strokeWidth = 2.dp)
            Text(stringResource(R.string.lesson_flow_reviewing), style = MaterialTheme.typography.bodyMedium)
        }
    }
    uiState.reviewProblem?.let { problem ->
        Text(
            stringResource(
                if (problem == ReviewProblem.OFFLINE) R.string.lesson_flow_review_offline else R.string.lesson_flow_review_unavailable,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    session.review(screen.id)?.let { ReviewCard(it) }
    if (session.isFinished) ModelAnswerCard(screen.model)
}

@Composable
private fun Footer(
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    Surface(tonalElevation = Dimens.spacingTiny) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            if (uiState.results != null) {
                uiState.nextLessonId?.let { next ->
                    Button(onClick = { actions.onNextLesson(next) }) { Text(stringResource(R.string.lesson_flow_next_lesson)) }
                }
                OutlinedButton(onClick = actions.onFinished) { Text(stringResource(R.string.lesson_flow_close)) }
                return@Row
            }
            if (session.canGoBack) OutlinedButton(onClick = actions.onBack) { Text(stringResource(R.string.lesson_flow_back)) }
            when {
                session.needsCheck ->
                    Button(onClick = actions.onCheck, enabled = session.canCheck && !uiState.isReviewing) {
                        Text(stringResource(R.string.exercise_check))
                    }

                session.isLastScreen -> Button(onClick = actions.onNext) { Text(stringResource(R.string.lesson_flow_finish)) }

                else -> Button(onClick = actions.onNext) { Text(stringResource(R.string.lesson_flow_next)) }
            }
        }
    }
}

private fun answerState(
    verdict: AnswerVerdict?,
    isChecked: Boolean,
): AnswerState =
    when {
        !isChecked || verdict == null -> AnswerState.Unanswered
        verdict == AnswerVerdict.CORRECT -> AnswerState.Correct
        else -> AnswerState.Incorrect()
    }
