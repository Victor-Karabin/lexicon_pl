package com.lexicon.application.numbers

import com.lexicon.boundary.NumberRepository
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.numbers.LoadNumberCourseUseCase
import com.lexicon.interactors.numbers.LoadNumberSpecUseCase
import com.lexicon.interactors.numbers.NextNumberUseCase
import com.lexicon.interactors.numbers.NumberCourse
import com.lexicon.interactors.numbers.NumberCourseProgress
import com.lexicon.interactors.numbers.NumberForm
import com.lexicon.interactors.numbers.NumberGender
import com.lexicon.interactors.numbers.NumberKind
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

private suspend fun NumberRepository.progressByForm(): Map<NumberForm, NumberProgress> =
    progress().mapNotNull {
        val kind = NumberKind.entries.firstOrNull { kind -> kind.name == it.kind } ?: return@mapNotNull null
        val gender = NumberGender.entries.firstOrNull { gender -> gender.name == it.gender }
        val form = NumberForm(it.value, kind, gender)
        form to NumberProgress(
            form = form,
            attempted = it.attempted,
            correct = it.correct,
            incorrect = it.incorrect,
            streak = it.streak,
        )
    }.toMap()

class LoadNumberCourseUseCaseImpl(
    private val numbers: NumberRepository,
) : LoadNumberCourseUseCase {
    override suspend fun invoke(): NumberCourse? {
        val (spec, chosen) = numbers.chosenNumbers() ?: return null
        val progress = numbers.progressByForm()
        val forms = NumberForm.formsOf(chosen)
        return NumberCourse(
            spec = spec,
            progress = NumberCourseProgress(
                numbers = chosen.size,
                total = forms.size,
                mastered = forms.count { progress[it]?.isMastered == true },
                attempted = forms.count { (progress[it]?.attempted ?: 0) > 0 },
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
        asked: Set<NumberForm>,
        random: Random,
    ): NumberQuestion? {
        val (_, chosen) = numbers.chosenNumbers() ?: return null
        val progress = numbers.progressByForm()
        val forms = NumberForm.formsOf(chosen)

        val fresh = forms.filterNot { it in asked }.ifEmpty { forms }
        val pool = fresh.filterNot { progress[it]?.isMastered == true }.ifEmpty { fresh }
        val fewest = pool.minOf { progress[it]?.attempted ?: 0 }
        val form = pool.filter { (progress[it]?.attempted ?: 0) == fewest }.random(random)

        return NumberQuestion.of(form)
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
        numbers.recordAttempt(
            question.value,
            kind = question.kind.name,
            gender = question.gender?.name.orEmpty(),
            isCorrect = verdict == AnswerVerdict.CORRECT,
        )
        return verdict
    }
}
