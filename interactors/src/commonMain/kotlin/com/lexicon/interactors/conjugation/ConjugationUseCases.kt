package com.lexicon.interactors.conjugation

import com.lexicon.interactors.course.AnswerVerdict
import kotlinx.collections.immutable.ImmutableList
import kotlin.random.Random

interface LoadConjugationVerbsUseCase {
    suspend fun page(
        query: String = "",
        offset: Int = 0,
    ): VerbPage
}

fun interface DeleteConjugationVerbUseCase {
    suspend operator fun invoke(infinitive: String)
}

fun interface HasDeletedVerbsUseCase {
    suspend operator fun invoke(): Boolean
}

fun interface RestoreConjugationVerbsUseCase {
    suspend operator fun invoke()
}

interface LoadConjugationCoursesUseCase {
    suspend operator fun invoke(): ImmutableList<ConjugationCourse>
}

interface NextConjugationTableUseCase {
    suspend operator fun invoke(
        courseId: String,
        asked: Set<String> = emptySet(),
        random: Random = Random.Default,
    ): ConjugationTable?
}

data class SubmitConjugationAnswerRequest(
    val courseId: String,
    val table: ConjugationTable,
    val answers: Map<GrammaticalPerson, String?>,
)

data class SubmitConjugationAnswerResponse(
    val verdicts: Map<GrammaticalPerson, AnswerVerdict>,
) {
    val correctness: Map<GrammaticalPerson, Boolean> get() = verdicts.mapValues { it.value == AnswerVerdict.CORRECT }

    val allCorrect: Boolean get() = correctness.values.all { it }
}

interface SubmitConjugationAnswerUseCase {
    suspend operator fun invoke(request: SubmitConjugationAnswerRequest): SubmitConjugationAnswerResponse
}

interface EnsureVerbWordUseCase {
    suspend operator fun invoke(
        infinitive: String,
        translation: String?,
    ): Long?
}

interface LoadVerbImageChoicesUseCase {
    suspend operator fun invoke(
        infinitive: String,
        translation: String?,
        skip: Int = 0,
    ): ImmutableList<String>
}

interface ChooseVerbImageUseCase {
    suspend operator fun invoke(
        infinitive: String,
        translation: String?,
        imageUrl: String,
    )
}
