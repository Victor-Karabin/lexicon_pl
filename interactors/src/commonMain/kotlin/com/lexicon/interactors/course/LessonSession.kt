package com.lexicon.interactors.course

import com.lexicon.model.course.ChoiceQuestion
import com.lexicon.model.course.FirstTry
import com.lexicon.model.course.GrammarRule
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonQuestion
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.LessonStep
import com.lexicon.model.course.TranscriptUnlock
import com.lexicon.model.course.WritingReview
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlin.random.Random

data class Score(
    val correct: Int,
    val total: Int,
) {
    val percent: Int get() = if (total == 0) 100 else correct * 100 / total

    val isPassed: Boolean get() = correct * 100 >= total * PASS_MARK

    companion object {
        const val PASS_MARK = 80
    }
}

data class StepScore(
    val step: LessonStep,
    val score: Score,
)

data class ExerciseToImprove(
    val screen: LessonScreen,
    val firstTry: FirstTry,
    val rules: List<GrammarRule>,
)

data class LessonResults(
    val steps: List<StepScore>,
    val toImprove: List<ExerciseToImprove> = emptyList(),
    val missedPolishLetters: Int = 0,
) {
    val overall: Score get() = Score(steps.sumOf { it.score.correct }, steps.sumOf { it.score.total })

    val rulesToRevisit: List<GrammarRule> get() = toImprove.flatMap { it.rules }.distinctBy { it.number }.sortedBy { it.number }
}

data class LessonSession(
    val script: LessonScript,
    val progress: LessonProgress = LessonProgress(),
) {
    val index: Int
        get() = progress.screenId?.let { id -> script.screens.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
            ?: progress.screenIndex.coerceIn(0, script.screens.lastIndex)

    val screen: LessonScreen get() = script.screens[index]

    val position: Int get() = index + 1

    val total: Int get() = script.screens.size

    val isChecked: Boolean get() = isChecked(screen.id)

    val isFinished: Boolean get() = screen.id in progress.finished || isChecked

    val canCheck: Boolean
        get() =
            when (val current = screen) {
                is LessonScreen.FreeWriting -> !isFinished && current.fields.indices.any { answer(current.id, it.toString()).isNotBlank() }
                else -> current.isGraded && !isChecked && isFullyAnswered
            }

    val isFullyAnswered: Boolean
        get() =
            when (val current = screen) {
                is LessonScreen.FreeWriting -> current.fields.indices.all { answer(current.id, it.toString()).isNotBlank() }
                else -> current.questions.all { answer(current.id, it.key).isNotBlank() }
            }

    val needsCheck: Boolean get() = !isFinished && (screen.isGraded || screen is LessonScreen.FreeWriting)

    val canGoBack: Boolean get() = index > 0

    val isLastScreen: Boolean get() = index == script.screens.lastIndex

    val isAtEnd: Boolean get() = isLastScreen && isFinished && failedScore == null

    val results: LessonResults
        get() = LessonResults(
            steps = script.steps.map { StepScore(it, stepScore(it)) }.filter { it.score.total > 0 },
            toImprove = script.screens
                .mapNotNull { screen -> progress.firstTries[screen.id]?.let { ExerciseToImprove(screen, it, script.rulesOf(screen.id)) } }
                .filter { it.firstTry.correct < it.firstTry.total }
                .sortedBy { it.firstTry.correct.toFloat() / it.firstTry.total },
            missedPolishLetters = progress.firstTries.values.sumOf { it.almost },
        )

    val failedScore: Score?
        get() = screenScore(screen).takeIf { screen.isGraded && isChecked && !it.isPassed }

    fun isChecked(screenId: String): Boolean = screenId in progress.checked

    fun answer(
        screenId: String,
        key: String,
    ): String = progress.answers[screenId]?.get(key).orEmpty()

    fun review(screenId: String): WritingReview? = progress.reviews[screenId]

    fun withAnswer(
        key: String,
        value: String,
    ): LessonSession {
        if (isFinished) return this
        val screenAnswers = (progress.answers[screen.id] ?: persistentMapOf()) + (key to value)
        return copy(progress = progress.copy(answers = (progress.answers + (screen.id to screenAnswers.toImmutableMap())).toImmutableMap()))
    }

    fun withPositionToggled(key: String): LessonSession {
        val lines = (screen as? LessonScreen.Ordering)?.lines ?: return this
        if (answer(screen.id, key).isNotEmpty()) return withAnswer(key, "")
        val taken = lines.map { answer(screen.id, it.key) }.toSet()
        val next = (1..lines.size).firstOrNull { it.toString() !in taken } ?: return this
        return withAnswer(key, next.toString())
    }

    fun check(): LessonSession {
        if (!canCheck) return this
        if (screen is LessonScreen.FreeWriting) return finish()
        val checked = copy(progress = progress.copy(checked = (progress.checked + screen.id).toImmutableSet()))
        if (screen.id in progress.firstTries) return checked
        val verdicts = screen.questions.map { checked.verdict(screen.id, it.key) }
        val first = FirstTry(
            correct = verdicts.count { it == AnswerVerdict.CORRECT },
            total = verdicts.size,
            almost = verdicts.count { it == AnswerVerdict.ALMOST },
        )
        return checked.copy(progress = checked.progress.copy(firstTries = (progress.firstTries + (screen.id to first)).toImmutableMap()))
    }

    fun withReview(review: WritingReview): LessonSession {
        if (screen !is LessonScreen.FreeWriting) return this
        return copy(progress = progress.copy(reviews = (progress.reviews + (screen.id to review)).toImmutableMap())).finish()
    }

    fun next(): LessonSession {
        if (needsCheck) return this
        if (failedScore != null) return retry()
        val done = finish()
        return if (done.isLastScreen) done else done.at(index + 1)
    }

    fun previous(): LessonSession = if (canGoBack) at(index - 1) else this

    private fun at(screenIndex: Int): LessonSession =
        copy(progress = progress.copy(screenIndex = screenIndex, screenId = script.screens[screenIndex].id))

    fun verdict(
        screenId: String,
        key: String,
    ): AnswerVerdict? {
        if (!isChecked(screenId)) return null
        val screen = script.screens.firstOrNull { it.id == screenId } ?: return null
        val question = screen.questions.firstOrNull { it.key == key } ?: return null
        val verdict = verdictOf(assigned(screen, question), answer(screenId, key))
        return if (verdict == AnswerVerdict.CORRECT && usedBefore(screen, key)) AnswerVerdict.WRONG else verdict
    }

    private fun usedBefore(
        screen: LessonScreen,
        key: String,
    ): Boolean {
        if (!screen.eachAnswerOnce) return false
        val given = answer(screen.id, key)
        return screen.questions.takeWhile { it.key != key }.any { earlier ->
            LessonAnswerChecker.same(answer(screen.id, earlier.key), given) &&
                verdictOf(assigned(screen, earlier), answer(screen.id, earlier.key)) == AnswerVerdict.CORRECT
        }
    }

    fun expectedAnswer(
        screenId: String,
        key: String,
    ): String {
        val screen = script.screens.firstOrNull { it.id == screenId } ?: return ""
        val question = screen.questions.firstOrNull { it.key == key } ?: return ""
        val answers = assigned(screen, question).answers
        if (usedBefore(screen, key)) {
            val taken = screen.questions.filter { it.key != key }.map { answer(screenId, it.key) }
            return answers.firstOrNull { candidate -> taken.none { LessonAnswerChecker.same(it, candidate) } } ?: answers.first()
        }
        return LessonAnswerChecker.written(LessonAnswerChecker.closest(answers, answer(screenId, key)))
    }

    fun usualForm(
        screenId: String,
        key: String,
    ): String? {
        if (verdict(screenId, key) != AnswerVerdict.CORRECT) return null
        val screen = script.screens.firstOrNull { it.id == screenId } ?: return null
        val question = assigned(screen, screen.questions.firstOrNull { it.key == key } ?: return null)
        val asWritten = LessonAnswerChecker.verdict(question.answers, answer(screenId, key), question.match)
        return LessonAnswerChecker.written(question.answers.first()).takeIf { asWritten != AnswerVerdict.CORRECT }
    }

    private fun verdictOf(
        question: LessonQuestion,
        given: String,
    ): AnswerVerdict = LessonAnswerChecker.verdict(question.answers + question.variants, given, question.match)

    private fun assigned(
        screen: LessonScreen,
        question: LessonQuestion,
    ): LessonQuestion {
        val group = screen.interchangeable.firstOrNull { question.key in it } ?: return question
        val members = group.mapNotNull { key -> screen.questions.firstOrNull { it.key == key } }
        val free = members.toMutableList()
        val assigned = mutableMapOf<String, LessonQuestion>()
        members.forEach { member ->
            val given = answer(screen.id, member.key)
            free.firstOrNull { verdictOf(it, given) == AnswerVerdict.CORRECT }?.let {
                assigned[member.key] = it
                free.remove(it)
            }
        }
        members.filter { it.key !in assigned }.zip(free).forEach { (member, left) -> assigned[member.key] = left }
        return assigned[question.key] ?: question
    }

    fun optionsOf(
        screen: LessonScreen.Choice,
        item: ChoiceQuestion,
        visit: Int,
    ): List<String> {
        if (!screen.shuffle || screen.legend.isNotEmpty()) return item.options
        val shared = screen.items.all { it.options == item.options }
        val seed = listOfNotNull(visit, script.lessonId.value, screen.id, item.question.key.takeUnless { shared }).joinToString("/")
        return item.options.shuffled(Random(seed.hashCode()))
    }

    fun screenScore(screen: LessonScreen): Score =
        Score(
            correct = screen.questions.count { verdict(screen.id, it.key) == AnswerVerdict.CORRECT },
            total = screen.questions.size,
        )

    fun isTranscriptUnlocked(screen: LessonScreen): Boolean = isUnlocked(screen.transcript?.unlock, screen)

    private fun isUnlocked(
        unlock: TranscriptUnlock?,
        screen: LessonScreen,
    ): Boolean =
        when (unlock) {
            null -> false
            TranscriptUnlock.Always -> true
            TranscriptUnlock.AfterCheck -> isChecked(screen.id) || screen.id in progress.finished
            is TranscriptUnlock.AfterScreen -> isChecked(unlock.screenId)
        }

    private fun finish(): LessonSession {
        if (screen.id in progress.finished) return this
        return copy(progress = progress.copy(finished = (progress.finished + screen.id).toImmutableSet()))
    }

    private fun retry(): LessonSession {
        val wrong = screen.questions.filter { verdict(screen.id, it.key) != AnswerVerdict.CORRECT }.map { it.key }.toSet()
        val kept = progress.answers[screen.id]?.let { (it - wrong).toImmutableMap() }
        return copy(
            progress = progress.copy(
                answers = (if (kept == null) progress.answers else progress.answers + (screen.id to kept)).toImmutableMap(),
                checked = (progress.checked - screen.id).toImmutableSet(),
                finished = (progress.finished - screen.id).toImmutableSet(),
            ),
        )
    }

    private fun stepScore(step: LessonStep): Score {
        val graded = script.screens.filter { it.id in step.screenIds && it.isGraded }
        return Score(graded.sumOf { screenScore(it).correct }, graded.sumOf { it.questions.size })
    }

    companion object {
        fun resume(
            script: LessonScript,
            progress: LessonProgress?,
        ): LessonSession {
            val saved = LessonSession(script, progress ?: LessonProgress())
            return if (saved.isAtEnd) LessonSession(script) else saved
        }

        fun isInProgress(
            script: LessonScript,
            progress: LessonProgress?,
        ): Boolean = progress != null && !LessonSession(script, progress).isAtEnd
    }
}
