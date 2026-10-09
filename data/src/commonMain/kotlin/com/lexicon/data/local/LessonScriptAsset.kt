package com.lexicon.data.local

import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.ChoiceQuestion
import com.lexicon.model.course.GapLine
import com.lexicon.model.course.GapSection
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonPhrase
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.LessonStep
import com.lexicon.model.course.LessonTable
import com.lexicon.model.course.LessonTrack
import com.lexicon.model.course.Transcript
import com.lexicon.model.course.TranscriptLine
import com.lexicon.model.course.TranscriptSection
import com.lexicon.model.course.TranscriptUnlock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val AFTER_SCREEN = "after_screen:"

@Serializable
data class LessonScriptAsset(
    val lessonId: String,
    val title: String,
    val passMark: Double = 0.8,
    val tracks: List<TrackAsset> = emptyList(),
    val steps: List<StepAsset> = emptyList(),
    val screens: List<ScreenAsset> = emptyList(),
    val vocabulary: List<PhraseAsset> = emptyList(),
)

@Serializable
data class TrackAsset(
    val id: String,
    val file: String,
)

@Serializable
data class StepAsset(
    val id: String,
    val title: String,
    val screens: List<String>,
)

@Serializable
data class TrackRefAsset(
    val id: String,
    val label: String? = null,
)

@Serializable
data class TableAsset(
    val header: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
)

@Serializable
data class LineAsset(
    val speaker: String? = null,
    val text: String,
)

@Serializable
data class SectionAsset(
    val title: String? = null,
    val lines: List<LineAsset> = emptyList(),
)

@Serializable
data class TranscriptAsset(
    val unlock: String = "after_check",
    val sections: List<SectionAsset> = emptyList(),
)

@Serializable
data class ItemAsset(
    val label: String = "",
    val answers: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val answer: String? = null,
    val feedback: String? = null,
)

@Serializable
data class GapAsset(
    val number: Int,
    val answers: List<String>,
    val feedback: String? = null,
)

@Serializable
data class FieldAsset(
    val label: String,
)

@Serializable
data class ScreenAsset(
    val id: String,
    val type: String,
    val title: String,
    val instruction: String = "",
    val tracks: List<TrackRefAsset> = emptyList(),
    val transcript: TranscriptAsset? = null,
    val tables: List<TableAsset> = emptyList(),
    val notes: List<String> = emptyList(),
    val keyboard: String = "text",
    val items: List<ItemAsset> = emptyList(),
    val sections: List<SectionAsset> = emptyList(),
    val gaps: List<GapAsset> = emptyList(),
    val fields: List<FieldAsset> = emptyList(),
    val model: List<String> = emptyList(),
    val checklist: List<String> = emptyList(),
)

@Serializable
data class PhraseAsset(
    val polish: String,
    val english: String,
)

@Serializable
data class LessonProgressAsset(
    val screenIndex: Int = 0,
    val answers: Map<String, Map<String, String>> = emptyMap(),
    val checked: Set<String> = emptySet(),
    val finished: Set<String> = emptySet(),
)

val lessonJson = Json { ignoreUnknownKeys = true }

fun LessonScriptAsset.toModel(remoteIds: Map<String, String?>): LessonScript {
    val files = tracks.associate { it.id to it.file }

    fun track(ref: TrackRefAsset): LessonTrack? =
        files[ref.id]?.let { file -> LessonTrack(id = ref.id, label = ref.label, file = file, remoteId = remoteIds[file]) }

    return LessonScript(
        lessonId = LessonId(lessonId),
        title = title,
        passMark = passMark,
        steps = steps.map { LessonStep(it.id, it.title, it.screens.toImmutableList()) }.toImmutableList(),
        screens = screens.map { it.toModel(::track) }.toImmutableList(),
        vocabulary = vocabulary.map { LessonPhrase(it.polish, it.english) }.toImmutableList(),
    )
}

private fun ScreenAsset.toModel(track: (TrackRefAsset) -> LessonTrack?): LessonScreen {
    val trackList = tracks.mapNotNull(track).toImmutableList()
    val transcriptModel = transcript?.toModel()
    return when (type) {
        "write" ->
            LessonScreen.Write(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                keyboard = if (keyboard == "digits") AnswerKeyboard.DIGITS else AnswerKeyboard.TEXT,
                questions = items.mapIndexed { index, item ->
                    LessonQuestion(
                        key = index.toString(),
                        label = item.label,
                        answers = item.answers.toImmutableList(),
                        feedback = item.feedback,
                    )
                }.toImmutableList(),
            )

        "choice" ->
            LessonScreen.Choice(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                items = items.mapIndexed { index, item ->
                    ChoiceQuestion(
                        question = LessonQuestion(
                            key = index.toString(),
                            label = item.label,
                            answers = listOfNotNull(item.answer).toImmutableList(),
                            feedback = item.feedback,
                        ),
                        options = item.options.toImmutableList(),
                    )
                }.toImmutableList(),
            )

        "gap_fill" ->
            LessonScreen.GapFill(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                sections = sections.map { section ->
                    GapSection(section.title, section.lines.map { GapLine(it.speaker, it.text) }.toImmutableList())
                }.toImmutableList(),
                questions = gaps.sortedBy { it.number }.map { gap ->
                    LessonQuestion(
                        key = gap.number.toString(),
                        label = gap.number.toString(),
                        answers = gap.answers.toImmutableList(),
                        feedback = gap.feedback,
                    )
                }.toImmutableList(),
            )

        "free_writing" ->
            LessonScreen.FreeWriting(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                fields = fields.map { it.label }.toImmutableList(),
                model = model.toImmutableList(),
                checklist = checklist.toImmutableList(),
            )

        else ->
            LessonScreen.Reference(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                tables = tables.map { table ->
                    LessonTable(table.header.toImmutableList(), table.rows.map { it.toImmutableList() }.toImmutableList())
                }.toImmutableList(),
                notes = notes.toImmutableList(),
            )
    }
}

private fun TranscriptAsset.toModel(): Transcript =
    Transcript(
        unlock = when {
            unlock == "always" -> TranscriptUnlock.Always
            unlock.startsWith(AFTER_SCREEN) -> TranscriptUnlock.AfterScreen(unlock.removePrefix(AFTER_SCREEN))
            else -> TranscriptUnlock.AfterCheck
        },
        sections = sections.map { section ->
            TranscriptSection(section.title, section.lines.map { TranscriptLine(it.speaker, it.text) }.toImmutableList())
        }.toImmutableList(),
    )

fun LessonProgress.toAsset(): LessonProgressAsset =
    LessonProgressAsset(
        screenIndex = screenIndex,
        answers = answers.mapValues { (_, values) -> values.toMap() },
        checked = checked.toSet(),
        finished = finished.toSet(),
    )

fun LessonProgressAsset.toModel(): LessonProgress =
    LessonProgress(
        screenIndex = screenIndex,
        answers = answers.mapValues { (_, values) -> values.toImmutableMap() }.toImmutableMap(),
        checked = checked.toImmutableSet(),
        finished = finished.toImmutableSet(),
    )
