package com.lexicon.domain.conjugation

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.ConjugationCourseProgress
import com.lexicon.interactors.conjugation.ConjugationQuestion
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.ConjugationVariantProgress
import com.lexicon.interactors.conjugation.FavouriteVerbUseCase
import com.lexicon.interactors.conjugation.LoadConjugationProgressUseCase
import com.lexicon.interactors.conjugation.LoadConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.LoadFavouriteVerbsUseCase
import com.lexicon.interactors.conjugation.LoadSelectedVerbsUseCase
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationQuestionUseCase
import com.lexicon.interactors.conjugation.ResetConjugationCourseUseCase
import com.lexicon.interactors.conjugation.SelectConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerRequest
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerResponse
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.interactors.conjugation.VerbConjugation
import com.lexicon.interactors.presets.CreateWordUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class LoadConjugationVerbsUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : LoadConjugationVerbsUseCase {
    override suspend fun invoke(query: String): ImmutableList<VerbConjugation> {
        val needle = query.trim()

        return conjugations
            .verbs()
            .map { it.toVerb() }
            .filter { it.isTeachable }
            .filter { needle.isBlank() || it.matches(needle) }
            .toImmutableList()
    }

    private fun VerbConjugation.matches(needle: String): Boolean =
        infinitive.contains(needle, ignoreCase = true) || translation?.contains(needle, ignoreCase = true) == true
}

class LoadSelectedVerbsUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : LoadSelectedVerbsUseCase {
    override suspend fun invoke() = conjugations.selectedInfinitives().toImmutableList()
}

class SelectConjugationVerbsUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : SelectConjugationVerbsUseCase {
    override suspend fun invoke(infinitives: List<String>) {
        val teachable = conjugations
            .verbs()
            .filter { it.toVerb().isTeachable }
            .map { it.infinitive }
            .toSet()

        conjugations.selectInfinitives(infinitives.filter { it in teachable })
    }
}

class LoadConjugationProgressUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : LoadConjugationProgressUseCase {
    override suspend fun invoke(): ConjugationCourseProgress = conjugations.courseProgress()
}

class ResetConjugationCourseUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : ResetConjugationCourseUseCase {
    override suspend fun invoke() = conjugations.resetProgress()
}

class NextConjugationQuestionUseCaseImpl(
    private val conjugations: ConjugationRepository,
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : NextConjugationQuestionUseCase {
    override suspend fun invoke(): ConjugationQuestion? {
        val selected = conjugations.selectedVerbs()
        if (selected.isEmpty()) return null

        val progress = conjugations.courseProgress().variants.associateBy { it.variant }
        val variant = selected.leastPractised(progress) ?: return null
        val verb = selected.first { it.infinitive == variant.infinitive }

        return verb.question(variant.person, selected)?.withLearningAids()
    }

    /** The variant the learner has done least well at, so weak spots come round again. */
    private fun List<VerbConjugation>.leastPractised(progress: Map<ConjugationVariant, ConjugationVariantProgress>): ConjugationVariant? =
        flatMap { verb -> verb.persons.map { ConjugationVariant(verb.infinitive, it) } }
            .filterNot { progress[it]?.isMastered == true }
            .minByOrNull { progress[it]?.attempted ?: 0 }
            ?: flatMap { verb -> verb.persons.map { ConjugationVariant(verb.infinitive, it) } }.randomOrNull()

    private suspend fun ConjugationQuestion.withLearningAids(): ConjugationQuestion {
        val word = runCatching { vocabulary.findWordByText(variant.infinitive) }.getOrNull()
        val subject = word?.translation?.takeIf { it.isNotBlank() } ?: variant.infinitive
        val image = conjugations.chosenImage(variant.infinitive)
            ?: runCatching { imageProvider.searchImage(subject) }.getOrNull()

        return copy(
            imageUrl = image,
            transcription = word?.transcription?.takeIf { it.isNotBlank() },
        )
    }
}

class LoadVerbImageChoicesUseCaseImpl(
    private val conjugations: ConjugationRepository,
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : LoadVerbImageChoicesUseCase {
    override suspend fun invoke(infinitive: String): ImmutableList<String> {
        val word = runCatching { vocabulary.findWordByText(infinitive) }.getOrNull()
        val subject = word?.translation?.takeIf { it.isNotBlank() } ?: infinitive
        val found = runCatching { imageProvider.searchImages(subject, CHOICE_COUNT) }.getOrDefault(emptyList())
        val chosen = conjugations.chosenImage(infinitive)

        return (listOfNotNull(chosen) + found).distinct().toImmutableList()
    }

    private companion object {
        private const val CHOICE_COUNT = 12
    }
}

class ChooseVerbImageUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : ChooseVerbImageUseCase {
    override suspend fun invoke(
        infinitive: String,
        imageUrl: String,
    ) = conjugations.chooseImage(infinitive, imageUrl)
}

class FavouriteVerbUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val createWord: CreateWordUseCase,
    private val imageProvider: ImageProvider,
) : FavouriteVerbUseCase {
    override suspend fun invoke(
        infinitive: String,
        translation: String?,
        isFavourite: Boolean,
    ) {
        vocabulary.findWordByText(infinitive)?.let { existing ->
            vocabulary.setFavourite(listOf(existing.id), isFavourite)
            return
        }

        if (!isFavourite) return

        val english = translation?.trim().orEmpty()
        if (english.isEmpty()) return

        val image = runCatching { imageProvider.searchImage(english) }.getOrNull()
        createWord(text = infinitive, translation = english, imageUrl = image, presetIds = emptyList())

        vocabulary.findWordByText(infinitive)?.let { vocabulary.setFavourite(listOf(it.id), true) }
    }
}

class LoadFavouriteVerbsUseCaseImpl(
    private val vocabulary: VocabularyRepository,
) : LoadFavouriteVerbsUseCase {
    override suspend fun invoke(infinitives: List<String>): Set<String> =
        infinitives
            .filter { vocabulary.findWordByText(it)?.isFavourite == true }
            .toSet()
}

class SubmitConjugationAnswerUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : SubmitConjugationAnswerUseCase {
    override suspend fun invoke(request: SubmitConjugationAnswerRequest): SubmitConjugationAnswerResponse {
        val given = request.answer?.trim()
        val isCorrect = given != null && request.question.correctOptions.any { it.equalsAnswer(given) }

        conjugations.recordAttempt(
            infinitive = request.question.variant.infinitive,
            person = request.question.variant.person.sourceKey,
            isCorrect = isCorrect,
        )

        return SubmitConjugationAnswerResponse(
            isCorrect = isCorrect,
            correctOptions = request.question.correctOptions,
        )
    }
}

private fun String.equalsAnswer(other: String): Boolean = trim().equals(other.trim(), ignoreCase = true)
