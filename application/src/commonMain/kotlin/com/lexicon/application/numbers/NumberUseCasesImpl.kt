package com.lexicon.application.numbers

import com.lexicon.boundary.NumberRepository
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.numbers.LoadNumberCourseUseCase
import com.lexicon.interactors.numbers.LoadNumberSpecUseCase
import com.lexicon.interactors.numbers.NextNumberUseCase
import com.lexicon.interactors.numbers.NumberCourse
import com.lexicon.interactors.numbers.NumberCourseProgress
import com.lexicon.interactors.numbers.NumberProgress
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.interactors.numbers.NumberSelection
import com.lexicon.interactors.numbers.SaveNumberCourseUseCase
import com.lexicon.interactors.numbers.SubmitNumberAnswerUseCase
import kotlin.random.Random

private suspend fun NumberRepository.chosenNumbers(): Pair<String, List<Long>>? {
    val spec = spec() ?: return null
    val selection = NumberSelection.parse(spec) as? NumberSelection.Valid ?: return null
    return spec to selection.numbers
}

private suspend fun NumberRepository.progressByValue(): Map<Long, NumberProgress> =
    progress().associate {
        it.value to NumberProgress(
            value = it.value,
            attempted = it.attempted,
            correct = it.correct,
            incorrect = it.incorrect,
            streak = it.streak,
        )
    }

class LoadNumberCourseUseCaseImpl(
    private val numbers: NumberRepository,
) : LoadNumberCourseUseCase {
    override suspend fun invoke(): NumberCourse? {
        val (spec, chosen) = numbers.chosenNumbers() ?: return null
        val progress = numbers.progressByValue()
        return NumberCourse(
            spec = spec,
            progress = NumberCourseProgress(
                total = chosen.size,
                mastered = chosen.count { progress[it]?.isMastered == true },
                attempted = chosen.count { (progress[it]?.attempted ?: 0) > 0 },
            ),
        )
    }
}

class LoadNumberSpecUseCaseImpl(
    private val numbers: NumberRepository,
) : LoadNumberSpecUseCase {
    override suspend fun invoke(): String = numbers.spec().orEmpty()
}

class SaveNumberCourseUseCaseImpl(
    private val numbers: NumberRepository,
) : SaveNumberCourseUseCase {
    override suspend fun invoke(spec: String): NumberSelection {
        val selection = NumberSelection.parse(spec)
        if (selection is NumberSelection.Valid) numbers.saveSpec(spec.trim())
        return selection
    }
}

class NextNumberUseCaseImpl(
    private val numbers: NumberRepository,
) : NextNumberUseCase {
    override suspend fun invoke(
        asked: Set<Long>,
        random: Random,
    ): NumberQuestion? {
        val (_, chosen) = numbers.chosenNumbers() ?: return null
        val progress = numbers.progressByValue()

        val fresh = chosen.filterNot { it in asked }.ifEmpty { chosen }
        val pool = fresh.filterNot { progress[it]?.isMastered == true }.ifEmpty { fresh }
        val fewest = pool.minOf { progress[it]?.attempted ?: 0 }
        val value = pool.filter { (progress[it]?.attempted ?: 0) == fewest }.random(random)

        return NumberQuestion.of(value)
    }
}

class SubmitNumberAnswerUseCaseImpl(
    private val numbers: NumberRepository,
) : SubmitNumberAnswerUseCase {
    override suspend fun invoke(
        question: NumberQuestion,
        typed: String,
    ): AnswerVerdict {
        val verdict = LessonAnswerChecker.verdict(question.answers, typed)
        numbers.recordAttempt(question.value, isCorrect = verdict == AnswerVerdict.CORRECT)
        return verdict
    }
}
