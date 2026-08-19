package com.lexicon.interactors.conjugation

import kotlinx.collections.immutable.ImmutableList

interface LoadConjugationVerbsUseCase {
    suspend operator fun invoke(query: String = ""): ImmutableList<VerbConjugation>
}

interface DeleteConjugationVerbUseCase {
    suspend operator fun invoke(infinitive: String)
}

interface HasDeletedVerbsUseCase {
    suspend operator fun invoke(): Boolean
}

interface RestoreConjugationVerbsUseCase {
    suspend operator fun invoke()
}

interface CreateConjugationCourseUseCase {
    suspend operator fun invoke(infinitives: List<String>): String
}

interface LoadConjugationCoursesUseCase {
    suspend operator fun invoke(): ImmutableList<ConjugationCourse>
}

interface DeleteConjugationCourseUseCase {
    suspend operator fun invoke(courseId: String)
}

interface NextConjugationTableUseCase {
    suspend operator fun invoke(courseId: String): ConjugationTable?
}

data class SubmitConjugationAnswerRequest(
    val courseId: String,
    val table: ConjugationTable,
    val answers: Map<GrammaticalPerson, String?>,
)

data class SubmitConjugationAnswerResponse(
    val correctness: Map<GrammaticalPerson, Boolean>,
) {
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
    ): ImmutableList<String>
}

interface ChooseVerbImageUseCase {
    suspend operator fun invoke(
        infinitive: String,
        translation: String?,
        imageUrl: String,
    )
}

interface ToggleVerbInStudySetUseCase {
    suspend operator fun invoke(
        infinitive: String,
        translation: String?,
        isInStudySet: Boolean,
    )
}

interface LoadStudySetVerbsUseCase {
    suspend operator fun invoke(infinitives: List<String>): Set<String>
}

interface LoadConjugationProgressUseCase {
    suspend operator fun invoke(courseId: String): ConjugationCourseProgress
}
