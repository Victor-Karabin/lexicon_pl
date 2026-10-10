package com.lexicon.interactors.course

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

data class Score(
    val correct: Int,
    val total: Int,
) {
    val percent: Int get() = if (total == 0) 100 else correct * 100 / total
}

data class StepScore(
    val step: LessonStep,
    val score: Score,
)

data class LessonResults(
    val steps: List<StepScore>,
) {
    val overall: Score get() = Score(steps.sumOf { it.score.correct }, steps.sumOf { it.score.total })
}

data class LessonSession(
    val script: LessonScript,
    val progress: LessonProgress = LessonProgress(),
) {
    val index: Int get() = progress.screenIndex.coerceIn(0, script.screens.lastIndex)

    val screen: LessonScreen get() = script.screens[index]

    val position: Int get() = index + 1

    val total: Int get() = script.screens.size

    val isChecked: Boolean get() = isChecked(screen.id)

    val isFinished: Boolean get() = screen.id in progress.finished || isChecked

    val canCheck: Boolean
        get() =
            when (val current = screen) {
                is LessonScreen.FreeWriting -> !isFinished && current.fields.indices.any { answer(current.id, it.toString()).isNotBlank() }
                else -> current.isGraded && !isChecked && current.questions.all { answer(current.id, it.key).isNotBlank() }
            }

    val needsCheck: Boolean get() = !isFinished && (screen.isGraded || screen is LessonScreen.FreeWriting)

    val canGoBack: Boolean get() = index > 0

    val isLastScreen: Boolean get() = index == script.screens.lastIndex

    val isAtEnd: Boolean get() = isLastScreen && isFinished

    val results: LessonResults
        get() = LessonResults(script.steps.map { StepScore(it, stepScore(it)) }.filter { it.score.total > 0 })

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
        return copy(progress = progress.copy(checked = (progress.checked + screen.id).toImmutableSet()))
    }

    fun withReview(review: WritingReview): LessonSession {
        if (screen !is LessonScreen.FreeWriting) return this
        return copy(progress = progress.copy(reviews = (progress.reviews + (screen.id to review)).toImmutableMap())).finish()
    }

    fun next(): LessonSession {
        if (needsCheck) return this
        val done = finish()
        return if (done.isLastScreen) done else done.copy(progress = done.progress.copy(screenIndex = index + 1))
    }

    fun previous(): LessonSession = if (canGoBack) copy(progress = progress.copy(screenIndex = index - 1)) else this

    fun verdict(
        screenId: String,
        key: String,
    ): AnswerVerdict? {
        if (!isChecked(screenId)) return null
        val screen = script.screens.firstOrNull { it.id == screenId } ?: return null
        val question = screen.questions.firstOrNull { it.key == key } ?: return null
        return LessonAnswerChecker.verdict(accepted(screen, question), answer(screenId, key), question.match)
    }

    fun expectedAnswer(
        screenId: String,
        key: String,
    ): String {
        val screen = script.screens.firstOrNull { it.id == screenId } ?: return ""
        val question = screen.questions.firstOrNull { it.key == key } ?: return ""
        return LessonAnswerChecker.closest(accepted(screen, question), answer(screenId, key))
    }

    private fun accepted(
        screen: LessonScreen,
        question: LessonQuestion,
    ): List<String> {
        val group = screen.interchangeable.firstOrNull { question.key in it } ?: return question.answers
        val members = group.mapNotNull { key -> screen.questions.firstOrNull { it.key == key } }
        val free = members.toMutableList()
        val assigned = mutableMapOf<String, LessonQuestion>()
        members.forEach { member ->
            val given = answer(screen.id, member.key)
            free.firstOrNull { LessonAnswerChecker.verdict(it.answers, given, it.match) == AnswerVerdict.CORRECT }?.let {
                assigned[member.key] = it
                free.remove(it)
            }
        }
        members.filter { it.key !in assigned }.zip(free).forEach { (member, left) -> assigned[member.key] = left }
        return (assigned[question.key] ?: question).answers
    }

    fun screenScore(screen: LessonScreen): Score =
        Score(
            correct = screen.questions.count { verdict(screen.id, it.key) == AnswerVerdict.CORRECT },
            total = screen.questions.size,
        )

    fun isTranscriptUnlocked(screen: LessonScreen): Boolean =
        when (val unlock = screen.transcript?.unlock) {
            null -> false
            TranscriptUnlock.Always -> true
            TranscriptUnlock.AfterCheck -> isChecked(screen.id) || screen.id in progress.finished
            is TranscriptUnlock.AfterScreen -> isChecked(unlock.screenId)
        }

    private fun finish(): LessonSession {
        if (screen.id in progress.finished) return this
        return copy(progress = progress.copy(finished = (progress.finished + screen.id).toImmutableSet()))
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
