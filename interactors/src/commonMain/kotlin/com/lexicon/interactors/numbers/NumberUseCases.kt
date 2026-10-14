package com.lexicon.interactors.numbers

import com.lexicon.interactors.course.AnswerVerdict
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

data class NumberForm(
    val value: Long,
    val kind: NumberKind,
) {
    companion object {
        fun formsOf(numbers: List<Long>): List<NumberForm> =
            numbers.flatMap { value ->
                NumberKind.entries.filter { PolishNumbers.hasForm(value, it) }.map { NumberForm(value, it) }
            }
    }
}

data class NumberQuestion(
    val value: Long,
    val kind: NumberKind,
    val digits: String,
    val answers: ImmutableList<String>,
    val transcription: String,
    val rules: ImmutableList<NumberRule>,
) {
    val words: String get() = answers.first()

    companion object {
        fun of(
            value: Long,
            kind: NumberKind = NumberKind.CARDINAL,
        ): NumberQuestion =
            NumberQuestion(
                value = value,
                kind = kind,
                digits = PolishNumbers.digits(value, kind),
                answers = PolishNumbers.answers(value, kind).toImmutableList(),
                transcription = PolishNumbers.transcription(value, kind),
                rules = PolishNumbers.rules(value, kind).toImmutableList(),
            )

        fun of(form: NumberForm): NumberQuestion = of(form.value, form.kind)
    }

    val form: NumberForm get() = NumberForm(value, kind)
}

data class NumberProgress(
    val form: NumberForm,
    val attempted: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val streak: Int = 0,
) {
    val isMastered: Boolean get() = streak >= MASTERY_STREAK

    companion object {
        const val MASTERY_STREAK = 1
    }
}

data class NumberCourseProgress(
    val numbers: Int,
    val total: Int,
    val mastered: Int,
    val attempted: Int,
) {
    val fraction: Float get() = if (total == 0) 0f else mastered.toFloat() / total

    val isComplete: Boolean get() = total > 0 && mastered == total
}

data class NumberCourse(
    val spec: String,
    val progress: NumberCourseProgress,
)

interface LoadNumberCourseUseCase {
    suspend operator fun invoke(): NumberCourse?
}

interface LoadNumberSpecUseCase {
    suspend operator fun invoke(): String
}

interface SaveNumberCourseUseCase {
    suspend operator fun invoke(spec: String): NumberSelection
}

interface NextNumberUseCase {
    suspend operator fun invoke(
        asked: Set<NumberForm> = emptySet(),
        random: Random = Random.Default,
    ): NumberQuestion?
}

interface SubmitNumberAnswerUseCase {
    suspend operator fun invoke(
        question: NumberQuestion,
        typed: String,
    ): AnswerVerdict
}
