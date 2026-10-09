package com.lexicon.interactors.course

import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.LessonStep
import com.lexicon.model.course.TranscriptUnlock
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

data class ScreenScore(
    val correct: Int,
    val total: Int,
)

data class StepScore(
    val step: LessonStep,
    val correct: Int,
    val total: Int,
    val passMark: Double,
) {
    val passed: Boolean get() = total == 0 || correct >= passMark * total

    val percent: Int get() = if (total == 0) 100 else correct * 100 / total
}

data class LessonSession(
    val script: LessonScript,
    val progress: LessonProgress = LessonProgress(),
) {
    val index: Int get() = progress.screenIndex.coerceIn(0, script.screens.lastIndex)

    val screen: LessonScreen get() = script.screens[index]

    val position: Int get() = index + 1

    val total: Int get() = script.screens.size

    val step: LessonStep? get() = script.stepOf(screen.id)

    val isChecked: Boolean get() = isChecked(screen.id)

    val isFinished: Boolean get() = screen.id in progress.finished || isChecked

    val canCheck: Boolean
        get() = screen.isGraded && !isChecked && screen.questions.all { answer(screen.id, it.key).isNotBlank() }

    val canGoBack: Boolean get() = index > 0

    val endsStep: Boolean get() = step?.screenIds?.lastOrNull() == screen.id

    val isLastScreen: Boolean get() = index == script.screens.lastIndex

    val isLessonComplete: Boolean
        get() = isLastScreen && isFinished && script.steps.all { stepScore(it).passed && isStepChecked(it) }

    val stepToRetry: LessonStep?
        get() =
            when {
                !isFinished -> null
                endsStep && step?.let { !stepScore(it).passed } == true -> step
                isLastScreen -> script.steps.firstOrNull { !isStepChecked(it) || !stepScore(it).passed }
                else -> null
            }

    fun isChecked(screenId: String): Boolean = screenId in progress.checked

    fun answer(
        screenId: String,
        key: String,
    ): String = progress.answers[screenId]?.get(key).orEmpty()

    fun withAnswer(
        key: String,
        value: String,
    ): LessonSession {
        if (isChecked) return this
        val screenAnswers = (progress.answers[screen.id] ?: persistentMapOf()) + (key to value)
        return copy(progress = progress.copy(answers = (progress.answers + (screen.id to screenAnswers.toImmutableMap())).toImmutableMap()))
    }

    fun check(): LessonSession {
        if (!canCheck) return this
        return copy(progress = progress.copy(checked = (progress.checked + screen.id).toImmutableSet()))
    }

    fun finish(): LessonSession {
        if (screen.isGraded) return this
        return copy(progress = progress.copy(finished = (progress.finished + screen.id).toImmutableSet()))
    }

    fun next(): LessonSession {
        if (!isFinished || isLastScreen) return this
        return copy(progress = progress.copy(screenIndex = index + 1))
    }

    fun previous(): LessonSession = if (canGoBack) copy(progress = progress.copy(screenIndex = index - 1)) else this

    fun verdict(
        screenId: String,
        key: String,
    ): AnswerVerdict? {
        if (!isChecked(screenId)) return null
        val question = script.screens.firstOrNull { it.id == screenId }?.questions?.firstOrNull { it.key == key } ?: return null
        return LessonAnswerChecker.verdict(question.answers, answer(screenId, key))
    }

    fun screenScore(screen: LessonScreen): ScreenScore =
        ScreenScore(
            correct = screen.questions.count { verdict(screen.id, it.key) == AnswerVerdict.CORRECT },
            total = screen.questions.size,
        )

    fun stepScore(step: LessonStep): StepScore {
        val graded = screensOf(step).filter { it.isGraded }
        return StepScore(
            step = step,
            correct = graded.sumOf { screenScore(it).correct },
            total = graded.sumOf { it.questions.size },
            passMark = script.passMark,
        )
    }

    fun isStepChecked(step: LessonStep): Boolean = screensOf(step).filter { it.isGraded }.all { isChecked(it.id) }

    fun retryStep(step: LessonStep): LessonSession {
        val graded = screensOf(step).filter { it.isGraded }.map { it.id }.toSet()
        val first = script.screens.indexOfFirst { it.id == step.screenIds.firstOrNull() }.coerceAtLeast(0)
        return copy(
            progress = progress.copy(
                screenIndex = first,
                answers = progress.answers.filterKeys { it !in graded }.toImmutableMap(),
                checked = progress.checked.filterNot { it in graded }.toImmutableSet(),
            ),
        )
    }

    fun isTranscriptUnlocked(screen: LessonScreen): Boolean =
        when (val unlock = screen.transcript?.unlock) {
            null -> false
            TranscriptUnlock.Always -> true
            TranscriptUnlock.AfterCheck -> isChecked(screen.id) || screen.id in progress.finished
            is TranscriptUnlock.AfterScreen -> isChecked(unlock.screenId)
        }

    private fun screensOf(step: LessonStep): List<LessonScreen> = script.screens.filter { it.id in step.screenIds }

    companion object {
        fun start(
            script: LessonScript,
            progress: LessonProgress?,
        ): LessonSession = LessonSession(script, progress ?: LessonProgress())
    }
}
