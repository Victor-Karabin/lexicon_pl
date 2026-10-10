package com.lexicon.data.local

import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.ChoiceQuestion
import com.lexicon.model.course.FormGroup
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
import com.lexicon.model.course.OrderLine
import com.lexicon.model.course.PhraseGroup
import com.lexicon.model.course.Transcript
import com.lexicon.model.course.TranscriptLine
import com.lexicon.model.course.TranscriptSection
import com.lexicon.model.course.TranscriptUnlock
import com.lexicon.model.course.WritingReview
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val AFTER_SCREEN = "after_screen:"

@Serializable
data class LessonScriptAsset(
    val lessonId: String,
    val tracks: List<TrackAsset> = emptyList(),
    val steps: List<StepAsset> = emptyList(),
    val screens: List<ScreenAsset> = emptyList(),
)

@Serializable
data class TrackAsset(
    val id: String,
    val file: String,
    val remoteId: String? = null,
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
    val track: TrackRefAsset? = null,
    val lines: List<LineAsset> = emptyList(),
)

@Serializable
data class GroupAsset(
    val title: String? = null,
    val track: TrackRefAsset? = null,
    val phrases: List<PhraseAsset> = emptyList(),
    val items: List<ItemAsset> = emptyList(),
)

@Serializable
data class TranscriptAsset(
    val unlock: String = "after_check",
    val open: Boolean = false,
    val sections: List<SectionAsset> = emptyList(),
)

@Serializable
data class ItemAsset(
    val label: String = "",
    val speaker: String? = null,
    val prompt: String? = null,
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
    val hint: String? = null,
    val legend: List<String> = emptyList(),
    val interchangeable: List<List<String>> = emptyList(),
    val tracks: List<TrackRefAsset> = emptyList(),
    val transcript: TranscriptAsset? = null,
    val tables: List<TableAsset> = emptyList(),
    val groups: List<GroupAsset> = emptyList(),
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
data class ReviewAsset(
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
)

@Serializable
data class LessonProgressAsset(
    val screenIndex: Int = 0,
    val answers: Map<String, Map<String, String>> = emptyMap(),
    val checked: Set<String> = emptySet(),
    val finished: Set<String> = emptySet(),
    val reviews: Map<String, ReviewAsset> = emptyMap(),
)

val lessonJson = Json { ignoreUnknownKeys = true }

fun LessonScriptAsset.toModel(remoteIds: Map<String, String?>): LessonScript {
    val byId = tracks.associateBy { it.id }

    fun track(ref: TrackRefAsset): LessonTrack? =
        byId[ref.id]?.let { asset ->
            LessonTrack(id = ref.id, label = ref.label, file = asset.file, remoteId = remoteIds[asset.file] ?: asset.remoteId)
        }

    return LessonScript(
        lessonId = LessonId(lessonId),
        steps = steps.map { LessonStep(it.id, it.title, it.screens.toImmutableList()) }.toImmutableList(),
        screens = screens.map { it.toModel(::track) }.toImmutableList(),
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
                questions = items.mapIndexed { index, item -> item.toQuestion(index.toString()) }.toImmutableList(),
                hint = hint,
                notes = notes.toImmutableList(),
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
                            prompt = item.prompt,
                        ),
                        options = item.options.toImmutableList(),
                    )
                }.toImmutableList(),
                legend = legend.toImmutableList(),
                hint = hint,
                interchangeable = interchangeable.map { labels ->
                    labels.map { label -> items.indexOfFirst { it.label == label }.toString() }.toImmutableList()
                }.toImmutableList(),
            )

        "order" ->
            LessonScreen.Ordering(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                lines = items.map { OrderLine(it.label, it.speaker, it.prompt.orEmpty()) }.toImmutableList(),
                questions = items.map { item ->
                    LessonQuestion(
                        key = item.label,
                        label = item.label,
                        answers = listOfNotNull(item.answer).toImmutableList(),
                        feedback = item.feedback,
                    )
                }.toImmutableList(),
                notes = notes.toImmutableList(),
            )

        "form" ->
            LessonScreen.Form(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                groups = groups.mapIndexed { group, asset ->
                    FormGroup(
                        title = asset.title.orEmpty(),
                        fields = asset.items.mapIndexed { index, item ->
                            item.toQuestion("$group-$index").copy(match = AnswerMatch.KEY_WORDS)
                        }.toImmutableList(),
                    )
                }.toImmutableList(),
                hint = hint,
            )

        "gap_fill" ->
            LessonScreen.GapFill(
                id = id,
                title = title,
                instruction = instruction,
                tracks = trackList,
                transcript = transcriptModel,
                sections = sections.map { section ->
                    GapSection(
                        title = section.title,
                        track = section.track?.let(track),
                        lines = section.lines.map { GapLine(it.speaker, it.text) }.toImmutableList(),
                    )
                }.toImmutableList(),
                questions = gaps.sortedBy { it.number }.map { gap ->
                    LessonQuestion(
                        key = gap.number.toString(),
                        label = gap.number.toString(),
                        answers = gap.answers.toImmutableList(),
                        feedback = gap.feedback,
                    )
                }.toImmutableList(),
                notes = notes.toImmutableList(),
                hint = hint,
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
                groups = groups.map { group ->
                    PhraseGroup(
                        title = group.title,
                        track = group.track?.let(track),
                        phrases = group.phrases.map { LessonPhrase(it.polish, it.english) }.toImmutableList(),
                    )
                }.toImmutableList(),
                notes = notes.toImmutableList(),
            )
    }
}

private fun ItemAsset.toQuestion(key: String): LessonQuestion =
    LessonQuestion(
        key = key,
        label = label,
        answers = answers.toImmutableList(),
        feedback = feedback,
        prompt = prompt,
    )

private fun TranscriptAsset.toModel(): Transcript =
    Transcript(
        unlock = when {
            unlock == "always" -> TranscriptUnlock.Always
            unlock.startsWith(AFTER_SCREEN) -> TranscriptUnlock.AfterScreen(unlock.removePrefix(AFTER_SCREEN))
            else -> TranscriptUnlock.AfterCheck
        },
        isOpen = open,
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
        reviews = reviews.mapValues { (_, review) -> ReviewAsset(review.strengths, review.improvements) },
    )

fun LessonProgressAsset.toModel(): LessonProgress =
    LessonProgress(
        screenIndex = screenIndex,
        answers = answers.mapValues { (_, values) -> values.toImmutableMap() }.toImmutableMap(),
        checked = checked.toImmutableSet(),
        finished = finished.toImmutableSet(),
        reviews = reviews.mapValues { (_, review) ->
            WritingReview(review.strengths.toImmutableList(), review.improvements.toImmutableList())
        }.toImmutableMap(),
    )
