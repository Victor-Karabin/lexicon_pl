package com.lexicon.interactors.conjugation

import kotlinx.collections.immutable.ImmutableList

interface LoadConjugationVerbsUseCase {
    suspend operator fun invoke(query: String = ""): ImmutableList<VerbConjugation>
}

interface LoadSelectedVerbsUseCase {
    suspend operator fun invoke(): ImmutableList<String>
}

interface SelectConjugationVerbsUseCase {
    suspend operator fun invoke(infinitives: List<String>)
}

interface NextConjugationQuestionUseCase {
    suspend operator fun invoke(): ConjugationQuestion?
}

data class SubmitConjugationAnswerRequest(
    val question: ConjugationQuestion,
    val answer: String?,
)

data class SubmitConjugationAnswerResponse(
    val isCorrect: Boolean,
    val correctOptions: ImmutableList<String>,
)

interface SubmitConjugationAnswerUseCase {
    suspend operator fun invoke(request: SubmitConjugationAnswerRequest): SubmitConjugationAnswerResponse
}

interface LoadVerbImageChoicesUseCase {
    suspend operator fun invoke(infinitive: String): ImmutableList<String>
}

interface ChooseVerbImageUseCase {
    suspend operator fun invoke(
        infinitive: String,
        imageUrl: String,
    )
}

interface FavouriteVerbUseCase {
    suspend operator fun invoke(
        infinitive: String,
        translation: String?,
        isFavourite: Boolean,
    )
}

interface LoadFavouriteVerbsUseCase {
    suspend operator fun invoke(infinitives: List<String>): Set<String>
}

interface LoadConjugationProgressUseCase {
    suspend operator fun invoke(): ConjugationCourseProgress
}

interface ResetConjugationCourseUseCase {
    suspend operator fun invoke()
}
