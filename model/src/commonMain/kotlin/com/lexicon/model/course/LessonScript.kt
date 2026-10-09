package com.lexicon.model.course

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

data class LessonScript(
    val lessonId: LessonId,
    val title: String,
    val passMark: Double,
    val steps: ImmutableList<LessonStep>,
    val screens: ImmutableList<LessonScreen>,
    val vocabulary: ImmutableList<LessonPhrase>,
) {
    fun stepOf(screenId: String): LessonStep? = steps.firstOrNull { screenId in it.screenIds }
}

data class LessonStep(
    val id: String,
    val title: String,
    val screenIds: ImmutableList<String>,
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
)

enum class AnswerKeyboard { TEXT, DIGITS }

data class LessonQuestion(
    val key: String,
    val label: String,
    val answers: ImmutableList<String>,
    val feedback: String?,
)

data class ChoiceQuestion(
    val question: LessonQuestion,
    val options: ImmutableList<String>,
)

data class GapLine(
    val speaker: String?,
    val text: String,
)

data class GapSection(
    val title: String?,
    val lines: ImmutableList<GapLine>,
)

sealed interface LessonScreen {
    val id: String
    val title: String
    val instruction: String
    val tracks: ImmutableList<LessonTrack>
    val transcript: Transcript?
    val questions: List<LessonQuestion>

    val isGraded: Boolean get() = questions.isNotEmpty()

    data class Reference(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val tables: ImmutableList<LessonTable>,
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
    ) : LessonScreen

    data class Choice(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val items: ImmutableList<ChoiceQuestion>,
    ) : LessonScreen {
        override val questions: List<LessonQuestion> get() = items.map { it.question }
    }

    data class GapFill(
        override val id: String,
        override val title: String,
        override val instruction: String,
        override val tracks: ImmutableList<LessonTrack>,
        override val transcript: Transcript?,
        val sections: ImmutableList<GapSection>,
        override val questions: ImmutableList<LessonQuestion>,
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

data class LessonPhrase(
    val polish: String,
    val english: String,
)

data class LessonProgress(
    val screenIndex: Int = 0,
    val answers: ImmutableMap<String, ImmutableMap<String, String>> = persistentMapOf(),
    val checked: ImmutableSet<String> = persistentSetOf(),
    val finished: ImmutableSet<String> = persistentSetOf(),
)

val GAP_PATTERN = Regex("""\[(\d+)]""")
