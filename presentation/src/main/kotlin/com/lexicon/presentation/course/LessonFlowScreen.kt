package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.GAP_MARKER
import com.lexicon.interactors.course.GapFillItem
import com.lexicon.interactors.course.LessonSession
import com.lexicon.interactors.course.MinimalPairItem
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.GAP_PATTERN
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonTrack
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

class LessonFlowActions(
    val onClose: () -> Unit,
    val onPlay: (LessonTrack) -> Unit,
    val onReplay: (LessonTrack) -> Unit,
    val onAnswerChanged: (String, String) -> Unit,
    val onCheck: () -> Unit,
    val onDone: () -> Unit,
    val onShowModel: () -> Unit,
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onRetryStep: () -> Unit,
    val onTranscriptToggled: () -> Unit,
    val onFinishLesson: () -> Unit,
)

@Composable
fun LessonFlowScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonFlowViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonFlowContent(
        uiState = uiState,
        actions = LessonFlowActions(
            onClose = onClose,
            onPlay = viewModel::onPlay,
            onReplay = viewModel::onReplay,
            onAnswerChanged = viewModel::onAnswerChanged,
            onCheck = viewModel::onCheck,
            onDone = viewModel::onDone,
            onShowModel = viewModel::onShowModel,
            onNext = viewModel::onNext,
            onBack = viewModel::onBack,
            onRetryStep = viewModel::onRetryStep,
            onTranscriptToggled = viewModel::onTranscriptToggled,
            onFinishLesson = viewModel::onFinishLesson,
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
            if (uiState.isCompleted) {
                session.script.title
            } else {
                stringResource(
                    R.string.lesson_flow_position,
                    session.screen.id,
                    session.position,
                    session.total,
                )
            }
        }
        else -> stringResource(R.string.course_title)
    }

    val letters = remember { LetterTarget() }
    val index = (uiState as? LessonFlowUiState.Loaded)?.session?.index
    LaunchedEffect(index) { letters.insert = null }

    CompositionLocalProvider(LocalLetterTarget provides letters) {
        Scaffold(
            modifier = modifier,
            topBar = { TrainingTopBar(title = title, onClose = actions.onClose) },
            bottomBar = { if (uiState is LessonFlowUiState.Loaded) Footer(uiState, actions, letters) },
        ) { padding ->
            when (uiState) {
                LessonFlowUiState.Loading ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                LessonFlowUiState.NotFound ->
                    Box(Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.lesson_not_found), style = MaterialTheme.typography.bodyMedium)
                    }

                is LessonFlowUiState.Loaded ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.spacingMedium),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                    ) {
                        if (uiState.isCompleted) Completed(uiState.session) else ScreenBody(uiState, actions)
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
    Tracks(screen.tracks, uiState, actions)

    when (screen) {
        is LessonScreen.Reference -> ReferenceBody(screen)
        is LessonScreen.Write -> WriteBody(screen, session, actions)
        is LessonScreen.Choice -> ChoiceBody(screen, session, actions)
        is LessonScreen.GapFill -> GapFillBody(screen, session, actions)
        is LessonScreen.FreeWriting -> FreeWritingBody(screen, session, actions)
    }

    if (session.isChecked) {
        val score = session.screenScore(screen)
        Text(
            stringResource(R.string.exercise_score, score.correct, score.total),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    screen.transcript?.let { transcript ->
        TranscriptCard(
            transcript = transcript,
            isUnlocked = session.isTranscriptUnlocked(screen),
            isOpen = uiState.transcriptOpen,
            onToggle = actions.onTranscriptToggled,
        )
    }
    if (session.endsStep && session.isFinished) {
        session.step?.let { StepScoreCard(session.stepScore(it)) }
    }
    session.stepToRetry?.takeIf { it != session.step }?.let { failed ->
        Text(
            text = stringResource(R.string.lesson_flow_retry_earlier, failed.title, (session.script.passMark * 100).toInt()),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tracks(
    tracks: List<LessonTrack>,
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    if (tracks.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        tracks.forEach { track ->
            Column {
                track.label?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                ExerciseAudioButton(
                    isPlaying = uiState.playingFile == track.file,
                    onClick = { actions.onPlay(track) },
                    onReplay = { actions.onReplay(track) }.takeIf { track.file == uiState.playingFile || track.file == uiState.pausedFile },
                )
            }
        }
    }
    if (tracks.any { it.file in uiState.missingAudio }) {
        Text(
            stringResource(R.string.exercise_audio_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReferenceBody(screen: LessonScreen.Reference) {
    screen.tables.forEach { LessonTableView(it) }
    screen.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun WriteBody(
    screen: LessonScreen.Write,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    screen.questions.forEach { question ->
        val verdict = session.verdict(screen.id, question.key)
        TranscribeRow(
            item = TranscribeItem(question.label, question.answers.first()),
            value = session.answer(screen.id, question.key),
            isCorrect = verdict?.let { it == AnswerVerdict.CORRECT },
            answerState = answerState(verdict, session.isChecked),
            onValueChanged = { actions.onAnswerChanged(question.key, it) },
            keyboardType = if (screen.keyboard == AnswerKeyboard.DIGITS) KeyboardType.Number else KeyboardType.Text,
            isAlmost = verdict == AnswerVerdict.ALMOST,
        )
        if (verdict != null) question.feedback?.let { Feedback(it) }
    }
}

@Composable
private fun ChoiceBody(
    screen: LessonScreen.Choice,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    screen.items.forEach { item ->
        val question = item.question
        val verdict = session.verdict(screen.id, question.key)
        MinimalPairRow(
            item = MinimalPairItem(question.label, item.options, question.answers.first()),
            selected = session.answer(screen.id, question.key).ifEmpty { null },
            answerState = answerState(verdict, session.isChecked),
            onSelect = { actions.onAnswerChanged(question.key, it) },
        )
        if (verdict != null) question.feedback?.let { Feedback(it) }
    }
}

@Composable
private fun GapFillBody(
    screen: LessonScreen.GapFill,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    val questions = screen.questions.associateBy { it.key }
    screen.sections.forEach { section ->
        section.title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
        val keysInSection = mutableListOf<String>()
        section.lines.forEach { line ->
            val keys = GAP_PATTERN.findAll(line.text).map { it.groupValues[1] }.toList()
            keysInSection += keys
            val verdicts = keys.map { session.verdict(screen.id, it) }
            GapFillRow(
                item = GapFillItem(
                    prompt = line.text.replace(GAP_PATTERN, GAP_MARKER),
                    answers = keys.map { questions[it]?.answers?.first().orEmpty() }.toImmutableList(),
                    speaker = line.speaker,
                ),
                values = keys.map { session.answer(screen.id, it) },
                correctness = if (session.isChecked) verdicts.map { it == AnswerVerdict.CORRECT } else emptyList(),
                almost = verdicts.map { it == AnswerVerdict.ALMOST },
                answerState = when {
                    !session.isChecked -> AnswerState.Unanswered
                    verdicts.all { it == AnswerVerdict.CORRECT } -> AnswerState.Correct
                    else -> AnswerState.Incorrect()
                },
                onValueChanged = { at, value -> actions.onAnswerChanged(keys[at], value) },
            )
        }
        val explained = mutableSetOf<String>()
        keysInSection.mapNotNull { questions[it] }.forEach { question ->
            ItemFeedback(
                label = question.label,
                verdict = session.verdict(screen.id, question.key),
                expected = question.answers.first(),
                feedback = question.feedback?.takeIf { explained.add(it) },
            )
        }
    }
}

@Composable
private fun FreeWritingBody(
    screen: LessonScreen.FreeWriting,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    FreeWritingFields(
        fields = screen.fields,
        values = screen.fields.indices.map { session.answer(screen.id, it.toString()) },
        onValueChanged = { index, value -> actions.onAnswerChanged(index.toString(), value) },
    )
    if (session.isFinished) SelfCheckCard(screen.model, screen.checklist)
}

@Composable
private fun Feedback(text: String) {
    Text(
        text = richText(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Dimens.spacingLarge),
    )
}

@Composable
private fun Completed(session: LessonSession) {
    Text(stringResource(R.string.lesson_flow_completed), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.lesson_flow_completed_body, session.script.title), style = MaterialTheme.typography.bodyMedium)
    session.script.vocabulary.forEach { phrase ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(phrase.polish, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(phrase.english, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Footer(
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
    letters: LetterTarget,
) {
    val session = uiState.session
    val screen = session.screen
    val takesWords =
        screen is LessonScreen.GapFill ||
            screen is LessonScreen.FreeWriting ||
            (screen as? LessonScreen.Write)?.keyboard == AnswerKeyboard.TEXT
    val acceptsText = takesWords && !session.isChecked && !(screen is LessonScreen.FreeWriting && session.isFinished)
    val insert = letters.insert
    val takesLetters = acceptsText && insert != null && WindowInsets.isImeVisible

    Surface(tonalElevation = Dimens.spacingTiny, modifier = Modifier.imePadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            if (takesLetters && insert != null) PolishLettersBar(onLetter = insert)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                if (uiState.isCompleted) {
                    Button(onClick = actions.onClose) { Text(stringResource(R.string.lesson_flow_done)) }
                    return@Row
                }
                if (session.canGoBack) OutlinedButton(onClick = actions.onBack) { Text(stringResource(R.string.lesson_flow_back)) }
                FooterAction(session, actions)
            }
        }
    }
}

@Composable
private fun FooterAction(
    session: LessonSession,
    actions: LessonFlowActions,
) {
    val screen = session.screen
    when {
        screen.isGraded && !session.isChecked ->
            Button(onClick = actions.onCheck, enabled = session.canCheck) { Text(stringResource(R.string.exercise_check)) }

        screen is LessonScreen.FreeWriting && !session.isFinished ->
            Button(
                onClick = actions.onShowModel,
                enabled = screen.fields.indices.any { session.answer(screen.id, it.toString()).isNotBlank() },
            ) { Text(stringResource(R.string.lesson_flow_show_model)) }

        !session.isFinished -> Button(onClick = actions.onDone) { Text(stringResource(R.string.lesson_flow_done)) }

        session.stepToRetry != null -> {
            Button(onClick = actions.onRetryStep) { Text(stringResource(R.string.lesson_flow_retry)) }
            TextButton(onClick = if (session.isLastScreen) actions.onClose else actions.onNext) {
                Text(stringResource(R.string.lesson_flow_continue_anyway))
            }
        }

        session.isLastScreen ->
            Button(
                onClick = actions.onFinishLesson,
                enabled = session.isLessonComplete,
            ) { Text(stringResource(R.string.lesson_flow_finish)) }

        else -> Button(onClick = actions.onNext) { Text(stringResource(R.string.lesson_flow_next)) }
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
