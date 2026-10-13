package com.lexicon.presentation.course

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.GAP_MARKER
import com.lexicon.interactors.course.GapFillItem
import com.lexicon.interactors.course.InlineChoiceGroup
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.course.LessonSession
import com.lexicon.interactors.course.MinimalPairItem
import com.lexicon.interactors.course.Score
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.ChoiceQuestion
import com.lexicon.model.course.GAP_PATTERN
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonTrack
import com.lexicon.model.course.Transcript
import com.lexicon.model.course.allTracks
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.BottomActionPanel
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.isTyping
import com.lexicon.presentation.conjugation.PersonFormRow
import com.lexicon.presentation.conjugation.VerbHeading
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.StatChip
import com.lexicon.presentation.theme.component.tileSkin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random

private val ProgressSize = 18.dp
private val GrammarButtonClearance = 56.dp
private const val BLANK = "…"
private val TableLabelWidth = 120.dp
private val TableLabelLength = 3..16

class LessonFlowActions(
    val onClose: () -> Unit,
    val onFinished: () -> Unit,
    val onNextLesson: (LessonId) -> Unit,
    val onPlay: (LessonTrack) -> Unit,
    val onSeek: (LessonTrack, Long) -> Unit,
    val onSpeedChanged: () -> Unit,
    val onAnswerChanged: (String, String) -> Unit,
    val onLineTapped: (String) -> Unit,
    val onCheck: () -> Unit,
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onTranscriptToggled: (String) -> Unit,
    val onRetryAcknowledged: () -> Unit,
    val onGrammarShown: (Int?) -> Unit,
    val onGrammarDismissed: () -> Unit,
    val onPronounce: (String) -> Unit,
    val onWordStatusCycled: (VocabularyId) -> Unit,
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
            onSpeedChanged = viewModel::onSpeedChanged,
            onAnswerChanged = viewModel::onAnswerChanged,
            onLineTapped = viewModel::onLineTapped,
            onCheck = viewModel::onCheck,
            onNext = viewModel::onNext,
            onBack = viewModel::onBack,
            onTranscriptToggled = viewModel::onTranscriptToggled,
            onRetryAcknowledged = viewModel::onRetryAcknowledged,
            onGrammarShown = viewModel::onGrammarShown,
            onGrammarDismissed = viewModel::onGrammarDismissed,
            onPronounce = viewModel::onPronounce,
            onWordStatusCycled = viewModel::onWordStatusCycled,
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
                else -> {
                    val grammar = session.script.stepOf(session.screen.id)?.isGrammar == true
                    val shown = if (grammar) stringResource(R.string.lesson_flow_grammar_badge) else session.screen.id
                    stringResource(R.string.lesson_flow_position, shown, session.position, session.total)
                }
            }
        }
        else -> stringResource(R.string.course_title)
    }

    (uiState as? LessonFlowUiState.Loaded)?.retry?.let { RetryDialog(it, actions.onRetryAcknowledged) }
    (uiState as? LessonFlowUiState.Loaded)?.let { loaded ->
        loaded.grammar?.let { sheet ->
            GrammarSheet(
                rules = loaded.session.script.rules,
                openRule = sheet.rule,
                onRuleShown = actions.onGrammarShown,
                onDismiss = actions.onGrammarDismissed,
            )
        }
    }

    val shownPlayers = remember((uiState as? LessonFlowUiState.Loaded)?.session?.index) { mutableStateMapOf<String, Boolean>() }

    AnswerInputs {
        val isTyping = isTyping()
        CompositionLocalProvider(LocalShownPlayers provides shownPlayers) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    val finished = (uiState as? LessonFlowUiState.Loaded)?.results != null
                    TrainingTopBar(title = title, onClose = if (finished) actions.onFinished else actions.onClose)
                },
                bottomBar = {
                    if (uiState is LessonFlowUiState.Loaded) {
                        Column {
                            PinnedPlayer(uiState, actions, shownPlayers)
                            if (uiState.results == null || uiState.nextLessonId != null) Footer(uiState, actions)
                        }
                    }
                },
                floatingActionButton = {
                    if (uiState is LessonFlowUiState.Loaded && uiState.showsGrammarButton && !isTyping) {
                        SmallFloatingActionButton(onClick = { actions.onGrammarShown(null) }) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = stringResource(R.string.lesson_flow_grammar))
                        }
                    }
                },
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
                                    .padding(Dimens.spacingMedium)
                                    .padding(bottom = if (isTyping) 0.dp else GrammarButtonClearance),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                            ) {
                                val results = uiState.results
                                if (results != null) {
                                    LessonResultsView(results, uiState.lessonTitle, onRuleOpened = actions.onGrammarShown)
                                } else {
                                    ScreenBody(uiState, actions)
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun RetryDialog(
    retry: Score,
    onAcknowledged: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAcknowledged,
        title = { Text(stringResource(R.string.lesson_flow_retry_title)) },
        text = {
            Text(
                stringResource(
                    R.string.lesson_flow_retry_body,
                    retry.correct,
                    retry.total,
                    retry.percent,
                    Score.PASS_MARK,
                ),
            )
        },
        confirmButton = { TextButton(onClick = onAcknowledged) { Text(stringResource(R.string.lesson_flow_retry_start)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScreenBody(
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    val screen = session.screen

    val step = session.script.stepOf(screen.id)
    if (step?.isGrammar == true) {
        StatChip(
            icon = Icons.Default.School,
            text = stringResource(R.string.lesson_flow_grammar_badge),
            skin = tileSkin(highlighted = true),
        )
    }
    if (step?.isReview == true) {
        StatChip(icon = Icons.Default.Replay, text = stringResource(R.string.lesson_flow_review_badge), skin = tileSkin(highlighted = true))
        if (step.screenIds.first() == screen.id) {
            Text(stringResource(R.string.lesson_flow_review_intro), style = MaterialTheme.typography.bodyMedium)
        }
    }
    Text(screen.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    val rules = session.script.rulesOf(screen.id)
    if (rules.isNotEmpty()) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
            ) {
                rules.forEach { rule -> RuleChip(rule, onClick = { actions.onGrammarShown(rule.number) }) }
            }
        }
    }
    session.script.newWords[screen.id]?.let { newWords ->
        if (session.isNewWordsUnlocked(screen)) {
            NewWordsCard(
                words = newWords.words,
                entries = uiState.newWordEntries,
                statuses = uiState.wordStatuses,
                onPronounce = actions.onPronounce,
                onStatusCycled = actions.onWordStatusCycled,
            )
        }
    }
    Text(richText(screen.instruction), style = MaterialTheme.typography.bodyMedium)
    screen.tracks.forEach { TrackPlayer(it, uiState, actions) }
    screen.hint?.let { HintCard(it) }

    when (screen) {
        is LessonScreen.Reference -> ReferenceBody(screen, uiState, actions)
        is LessonScreen.Write -> WriteBody(screen, session, actions)
        is LessonScreen.Choice -> ChoiceBody(screen, uiState, actions)
        is LessonScreen.GapFill -> GapFillBody(screen, uiState, actions)
        is LessonScreen.FreeWriting -> FreeWritingBody(screen, uiState, actions)
        is LessonScreen.Ordering -> OrderingBody(screen, session, actions)
        is LessonScreen.Form -> FormBody(screen, session, actions)
        is LessonScreen.Conjugation -> ConjugationBody(screen, session, actions)
        is LessonScreen.SelectionGrid -> SelectionGridBody(screen, session, actions)
        is LessonScreen.InlineChoice -> InlineChoiceBody(screen, session, actions)
        is LessonScreen.Grammar -> session.script.rules.forEach { RuleView(it) }
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
        if (score.correct < score.total) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
                rules.forEach { rule ->
                    TextLink(stringResource(R.string.lesson_flow_see_rule, rule.number)) { actions.onGrammarShown(rule.number) }
                }
            }
        }
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

private val LocalShownPlayers = compositionLocalOf<MutableMap<String, Boolean>> { mutableMapOf() }

@Composable
private fun PinnedPlayer(
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
    shownPlayers: Map<String, Boolean>,
) {
    val file = uiState.tracks.activeFile
    val track = file?.let { active -> uiState.session.screen.allTracks().firstOrNull { it.file == active } }
    val state = track?.let { uiState.tracks.of(it.file) }
    val pinned = uiState.results == null &&
        track != null &&
        state != null &&
        (state.isPlaying || state.positionMs > 0) &&
        shownPlayers[track.file] == false
    var last by remember { mutableStateOf<LessonTrack?>(null) }
    if (pinned) last = track
    AnimatedVisibility(
        visible = pinned,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        val shown = last ?: return@AnimatedVisibility
        Surface(tonalElevation = Dimens.spacingTiny) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
            ) {
                Text(
                    shown.label ?: uiState.session.screen.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AudioPlayerRow(
                    state = uiState.tracks.of(shown.file),
                    onPlayPause = { actions.onPlay(shown) },
                    onSeek = { actions.onSeek(shown, it) },
                    onSpeedChanged = actions.onSpeedChanged,
                )
            }
        }
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
    val shownPlayers = LocalShownPlayers.current
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny)) {
        if (showLabel) track.label?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
        val state = uiState.tracks.of(track.file)
        AudioPlayerRow(
            state = state,
            onPlayPause = { actions.onPlay(track) },
            onSeek = { actions.onSeek(track, it) },
            onSpeedChanged = actions.onSpeedChanged,
            modifier = Modifier.onGloballyPositioned { shownPlayers[track.file] = it.boundsInWindow().height > 0f },
        )
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
    if (screen.wordBox.isNotEmpty()) WordBoxCard(screen.wordBox)
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
private fun ConjugationBody(
    screen: LessonScreen.Conjugation,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    screen.tables.forEach { table ->
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            VerbHeading(infinitive = table.verb, translation = table.translation)
            table.rows.forEach { row ->
                val expected = session.expectedAnswer(screen.id, row.key)
                PersonFormRow(
                    person = row.label,
                    typed = session.answer(screen.id, row.key),
                    expected = expected,
                    verdict = session.verdict(screen.id, row.key).takeIf { session.isChecked(screen.id) },
                    onAnswerChanged = { actions.onAnswerChanged(row.key, it) },
                    onSpeak = { actions.onPronounce(expected) },
                    imeAction = imeActionOf(session, row.key),
                    info = row.feedback,
                )
            }
        }
    }
    screen.notes.forEach { Text(richText(it), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun SelectionGridBody(
    screen: LessonScreen.SelectionGrid,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    if (screen.legend.isNotEmpty()) {
        SectionCard(title = null) {
            screen.legend.forEach { Text(richText(it), style = MaterialTheme.typography.bodyLarge) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
        screen.questions.forEach { question ->
            val selected = LessonAnswerChecker.selection(session.answer(screen.id, question.key))
            SelectionGridRow(
                label = question.label,
                options = screen.options,
                columns = screen.columns,
                selected = selected,
                expected = LessonAnswerChecker.selection(question.answers.first()).takeIf { session.isChecked(screen.id) },
                onToggle = { option ->
                    val toggled = if (option.uppercase() in selected) selected - option.uppercase() else selected + option.uppercase()
                    actions.onAnswerChanged(
                        question.key,
                        screen.options.filter { it.uppercase() in toggled }.joinToString(" "),
                    )
                },
            )
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
        usualForm = session.usualForm(screenId, question.key),
        leading = question.picture?.let { picture -> { ItemPictureView(picture) } },
        imeAction = imeActionOf(session, question.key),
    )
}

@Composable
private fun InlineChoiceBody(
    screen: LessonScreen.InlineChoice,
    session: LessonSession,
    actions: LessonFlowActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        screen.lines.forEach { line ->
            val order = GAP_PATTERN.findAll(line.text).map { line.groups[it.groupValues[1].toInt() - 1] }.toList()
            InlineChoiceRow(
                label = line.label,
                fragments = line.text.split(GAP_PATTERN),
                groups = order.map { group ->
                    InlineChoiceGroup(
                        options = group.options,
                        selected = session.answer(screen.id, group.question.key).ifEmpty { null },
                        answer = session.expectedAnswer(screen.id, group.question.key),
                        info = group.question.feedback,
                    )
                },
                checked = session.isChecked,
                onSelect = { index, option -> actions.onAnswerChanged(order[index].question.key, option) },
            )
        }
    }
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
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    if (screen.legend.isNotEmpty()) {
        SectionCard(title = null) {
            screen.legend.forEach { Text(richText(it), style = MaterialTheme.typography.bodyLarge) }
        }
    }
    val visit = rememberSaveable(screen.id) { Random.nextInt() }
    val shown = remember(session.script.lessonId, screen, visit) {
        screen.items.associate { it.question.key to session.optionsOf(screen, it, visit).toImmutableList() }
    }
    ChoiceItems(screen.id, screen.items, shown, uiState, actions)
}

@Composable
private fun ChoiceItems(
    screenId: String,
    items: List<ChoiceQuestion>,
    shown: Map<String, ImmutableList<String>>,
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        items.forEach { item ->
            val question = item.question
            item.track?.let { TrackPlayer(it, uiState, actions) }
            val prompt = question.prompt
            if (prompt != null && question.picture == null) {
                InlineChoiceRow(
                    label = null,
                    fragments = if (BLANK in prompt) prompt.split(BLANK, limit = 2) else listOf(prompt, ""),
                    groups = listOf(
                        InlineChoiceGroup(
                            options = shown.getValue(question.key),
                            selected = session.answer(screenId, question.key).ifEmpty { null },
                            answer = session.expectedAnswer(screenId, question.key),
                            info = question.feedback,
                        ),
                    ),
                    checked = session.isChecked,
                    onSelect = { _, option -> actions.onAnswerChanged(question.key, option) },
                )
                return@forEach
            }
            MinimalPairRow(
                item = MinimalPairItem(
                    "",
                    shown.getValue(question.key),
                    session.expectedAnswer(screenId, question.key),
                ),
                selected = session.answer(screenId, question.key).ifEmpty { null },
                answerState = answerState(session.verdict(screenId, question.key), session.isChecked),
                onSelect = { actions.onAnswerChanged(question.key, it) },
                info = question.feedback,
                prompt = question.prompt,
                leading = question.picture?.let { picture -> { ItemPictureView(picture) } },
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
    if (screen.choices.isNotEmpty()) {
        ChoiceItems(screen.id, screen.choices, screen.choices.associate { it.question.key to it.options }, uiState, actions)
    }
    if (screen.wordBox.isNotEmpty()) WordBoxCard(screen.wordBox)
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
                    usualForms = keys.map { session.usualForm(screen.id, it) },
                    answerState = when {
                        !session.isChecked -> AnswerState.Unanswered
                        verdicts.all { it == AnswerVerdict.CORRECT } -> AnswerState.Correct
                        else -> AnswerState.Incorrect()
                    },
                    onValueChanged = { at, value -> actions.onAnswerChanged(keys[at], value) },
                    imeAction = keys.lastOrNull()?.let { imeActionOf(session, it) } ?: ImeAction.Next,
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
        multiline = screen.fields.indices.map(screen::isMultiline),
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
    if (session.isFinished && screen.model.isNotEmpty()) ModelAnswerCard(screen.model)
}

@Composable
private fun Footer(
    uiState: LessonFlowUiState.Loaded,
    actions: LessonFlowActions,
) {
    val session = uiState.session
    BottomActionPanel(insets = WindowInsets.navigationBars) {
        if (uiState.results != null) {
            uiState.nextLessonId?.let { next ->
                Button(onClick = { actions.onNextLesson(next) }) { Text(stringResource(R.string.lesson_flow_next_lesson)) }
            }
            return@BottomActionPanel
        }
        if (session.canGoBack) OutlinedButton(onClick = actions.onBack) { Text(stringResource(R.string.lesson_flow_back)) }
        when {
            session.needsCheck ->
                Button(onClick = actions.onCheck, enabled = session.canCheck && !uiState.isReviewing) {
                    Text(stringResource(R.string.exercise_check))
                }

            session.isLastScreen -> Button(onClick = actions.onNext) { Text(stringResource(R.string.lesson_flow_finish)) }

            session.screen.isGraded || session.screen is LessonScreen.FreeWriting ->
                Button(onClick = actions.onNext) { Text(stringResource(R.string.lesson_flow_next)) }

            else -> Button(onClick = actions.onNext) { Text(stringResource(R.string.lesson_flow_done)) }
        }
    }
}

private fun imeActionOf(
    session: LessonSession,
    key: String,
): ImeAction = if (session.screen.questions.lastOrNull()?.key == key) ImeAction.Done else ImeAction.Next

private fun answerState(
    verdict: AnswerVerdict?,
    isChecked: Boolean,
): AnswerState =
    when {
        !isChecked || verdict == null -> AnswerState.Unanswered
        verdict == AnswerVerdict.CORRECT -> AnswerState.Correct
        else -> AnswerState.Incorrect()
    }
