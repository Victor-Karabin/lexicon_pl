package com.lexicon.interactors.declension

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.model.vocabulary.GrammaticalCase
import kotlinx.collections.immutable.ImmutableList
import kotlin.random.Random

fun interface LoadDeclinableWordsUseCase {
    suspend fun page(
        query: String,
        offset: Int,
    ): DeclinablePage
}

fun interface LoadDeclensionCoursesUseCase {
    suspend operator fun invoke(): ImmutableList<DeclensionCourse>
}

interface NextDeclensionTableUseCase {
    suspend operator fun invoke(
        courseId: String,
        asked: Set<String> = emptySet(),
        random: Random = Random.Default,
    ): DeclensionTable?
}

data class SubmitDeclensionAnswerRequest(
    val courseId: String,
    val table: DeclensionTable,
    val answers: Map<GrammaticalCase, String?>,
)

data class SubmitDeclensionAnswerResponse(
    val verdicts: Map<GrammaticalCase, AnswerVerdict>,
) {
    val correctness: Map<GrammaticalCase, Boolean> get() = verdicts.mapValues { it.value == AnswerVerdict.CORRECT }

    val allCorrect: Boolean get() = correctness.values.all { it }
}

fun interface SubmitDeclensionAnswerUseCase {
    suspend operator fun invoke(request: SubmitDeclensionAnswerRequest): SubmitDeclensionAnswerResponse
}
