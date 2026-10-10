package com.lexicon.model.course

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

data class LessonScript(
    val lessonId: LessonId,
    val steps: ImmutableList<LessonStep>,
    val screens: ImmutableList<LessonScreen>,
    val newWords: ImmutableMap<String, NewWords> = persistentMapOf(),
) {
    fun stepOf(screenId: String): LessonStep? = steps.firstOrNull { screenId in it.screenIds }
}

data class LessonStep(
    val id: String,
    val title: String,
    val screenIds: ImmutableList<String>,
    val isReview: Boolean = false,
)

data class NewWords(
    val words: ImmutableList<LessonPhrase>,
    val unlock: TranscriptUnlock,
)

data class LessonTrack(
    val id: String,
    val label: String?,
    val file: String,
    val remoteId: String?,
)

data class LessonTable(
    val header: ImmutableList<String>,
    val rows: ImmutableList<ImmutableList<String>>,
)

data class TranscriptLine(
    val speaker: String?,
    val text: String,
)

data class TranscriptSection(
    val title: String?,
    val lines: ImmutableList<TranscriptLine>,
)

sealed interface TranscriptUnlock {
    data object Always : TranscriptUnlock

    data object AfterCheck : TranscriptUnlock

    data class AfterScreen(val screenId: String) : TranscriptUnlock
}

data class Transcript(
    val unlock: TranscriptUnlock,
    val sections: ImmutableList<TranscriptSection>,
    val isOpen: Boolean = false,
)

enum class AnswerKeyboard { TEXT, DIGITS }

enum class AnswerMatch { WHOLE, KEY_WORDS }

sealed interface ItemPicture {
    data class Symbol(
        val emoji: String?,
        val icon: String?,
        val label: String,
    ) : ItemPicture

    data class Swatch(val color: Long) : ItemPicture
}

data class LessonQuestion(
    val key: String,
    val label: String,
    val answers: ImmutableList<String>,
    val feedback: String?,
    val prompt: String? = null,
    val match: AnswerMatch = AnswerMatch.WHOLE,
    val variants: ImmutableList<String> = persistentListOf(),
    val picture: ItemPicture? = null,
)

data class ChoiceQuestion(
    val question: LessonQuestion,
    val options: ImmutableList<String>,
)

data class InlineChoiceLine(
    val label: String,
    val text: String,
    val groups: ImmutableList<ChoiceQuestion>,
)

data class GapLine(
    val speaker: String?,
    val text: String,
)

data class GapSection(
    val title: String?,
    val track: LessonTrack?,
    val lines: ImmutableList<GapLine>,
)

data class OrderLine(
    val key: String,
    val speaker: String?,
    val text: String,
)

data class FormGroup(
    val title: String,
    val fields: ImmutableList<LessonQuestion>,
)

data class PhraseGroup(
    val title: String?,
    val track: LessonTrack?,
    val phrases: ImmutableList<LessonPhrase>,
)

sealed interface LessonScreen {
    val id: String
    val title: String
    val instruction: String
    val tracks: ImmutableList<LessonTrack>
    val transcript: Transcript?
    val questions: List<LessonQuestion>

    val hint: String? get() = null

    val interchangeable: List<List<String>> get() = emptyList()

    val isGraded: Boolean get() = questions.isNotEmpty()

    data class Reference(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val tables: ImmutableList<LessonTable>,
        val groups: ImmutableList<PhraseGroup>,
        val notes: ImmutableList<String>,
    ) : LessonScreen {
        override val questions: List<LessonQuestion> get() = emptyList()
    }

    data class Write(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val keyboard: AnswerKeyboard,
        override val questions: ImmutableList<LessonQuestion>,
        override val hint: String? = null,
        val notes: ImmutableList<String> = persistentListOf(),
        val wordBox: ImmutableList<String> = persistentListOf(),
    ) : LessonScreen

    data class Choice(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val items: ImmutableList<ChoiceQuestion>,
        val legend: ImmutableList<String> = persistentListOf(),
        override val hint: String? = null,
        override val interchangeable: ImmutableList<ImmutableList<String>> = persistentListOf(),
        val shuffle: Boolean = true,
    ) : LessonScreen {
        override val questions: List<LessonQuestion> get() = items.map { it.question }
    }

    data class InlineChoice(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val lines: ImmutableList<InlineChoiceLine>,
    ) : LessonScreen {
        override val questions: List<LessonQuestion> get() = lines.flatMap { line -> line.groups.map { it.question } }
    }

    data class Ordering(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val lines: ImmutableList<OrderLine>,
        override val questions: ImmutableList<LessonQuestion>,
        val notes: ImmutableList<String> = persistentListOf(),
    ) : LessonScreen

    data class Form(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val groups: ImmutableList<FormGroup>,
        override val hint: String? = null,
    ) : LessonScreen {
        override val questions: List<LessonQuestion> get() = groups.flatMap { it.fields }
    }

    data class GapFill(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val sections: ImmutableList<GapSection>,
        override val questions: ImmutableList<LessonQuestion>,
        val notes: ImmutableList<String> = persistentListOf(),
        override val hint: String? = null,
        val wordBox: ImmutableList<String> = persistentListOf(),
        override val interchangeable: ImmutableList<ImmutableList<String>> = persistentListOf(),
    ) : LessonScreen

    data class FreeWriting(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val fields: ImmutableList<String>,
        val model: ImmutableList<String>,
        val checklist: ImmutableList<String>,
    ) : LessonScreen {
        override val questions: List<LessonQuestion> get() = emptyList()
    }
}

fun LessonScreen.allTracks(): List<LessonTrack> =
    tracks +
        when (this) {
            is LessonScreen.Reference -> groups.mapNotNull { it.track }
            is LessonScreen.GapFill -> sections.mapNotNull { it.track }
            else -> emptyList()
        }

data class LessonPhrase(
    val polish: String,
    val english: String,
)

data class WritingReview(
    val strengths: ImmutableList<String>,
    val improvements: ImmutableList<String>,
)

data class LessonProgress(
    val screenIndex: Int = 0,
    val answers: ImmutableMap<String, ImmutableMap<String, String>> = persistentMapOf(),
    val checked: ImmutableSet<String> = persistentSetOf(),
    val finished: ImmutableSet<String> = persistentSetOf(),
    val reviews: ImmutableMap<String, WritingReview> = persistentMapOf(),
)

val GAP_PATTERN = Regex("""\[(\d+)]""")
