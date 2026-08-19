package com.lexicon.application.conjugation

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.conjugation.ConjugationCourseProgress
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.ConjugationVariantProgress
import com.lexicon.interactors.conjugation.CreateConjugationCourseUseCase
import com.lexicon.interactors.conjugation.DeleteConjugationCourseUseCase
import com.lexicon.interactors.conjugation.DeleteConjugationVerbUseCase
import com.lexicon.interactors.conjugation.EnsureVerbWordUseCase
import com.lexicon.interactors.conjugation.HasDeletedVerbsUseCase
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.conjugation.LoadConjugationProgressUseCase
import com.lexicon.interactors.conjugation.LoadConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.LoadStudySetVerbsUseCase
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationTableUseCase
import com.lexicon.interactors.conjugation.RestoreConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerRequest
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerResponse
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.interactors.conjugation.ToggleVerbInStudySetUseCase
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

class DeleteConjugationVerbUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : DeleteConjugationVerbUseCase {
    override suspend fun invoke(infinitive: String) = conjugations.deleteVerb(infinitive)
}

class HasDeletedVerbsUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : HasDeletedVerbsUseCase {
    override suspend fun invoke(): Boolean = conjugations.hasDeletedVerbs()
}

class RestoreConjugationVerbsUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : RestoreConjugationVerbsUseCase {
    override suspend fun invoke() = conjugations.restoreVerbs()
}

class CreateConjugationCourseUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : CreateConjugationCourseUseCase {
    override suspend fun invoke(infinitives: List<String>): String {
        val teachable = conjugations
            .verbs()
            .filter { it.toVerb().isTeachable }
            .map { it.infinitive }
            .toSet()

        return conjugations.createCourse(infinitives.filter { it in teachable })
    }
}

class LoadConjugationCoursesUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : LoadConjugationCoursesUseCase {
    override suspend fun invoke(): ImmutableList<ConjugationCourse> =
        conjugations
            .courses()
            .map {
                ConjugationCourse(
                    id = it.id,
                    infinitives = it.infinitives.toImmutableList(),
                    progress = conjugations.courseProgress(it.id),
                )
            }.toImmutableList()
}

class DeleteConjugationCourseUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : DeleteConjugationCourseUseCase {
    override suspend fun invoke(courseId: String) = conjugations.deleteCourse(courseId)
}

class LoadConjugationProgressUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : LoadConjugationProgressUseCase {
    override suspend fun invoke(courseId: String): ConjugationCourseProgress = conjugations.courseProgress(courseId)
}

class NextConjugationTableUseCaseImpl(
    private val conjugations: ConjugationRepository,
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : NextConjugationTableUseCase {
    override suspend fun invoke(courseId: String): ConjugationTable? {
        val selected = conjugations.courseVerbs(courseId)
        if (selected.isEmpty()) return null

        val progress = conjugations.courseProgress(courseId).variants.associateBy { it.variant }
        val verb = selected.leastPractised(progress) ?: return null

        return verb.question(selected)?.withLearningAids()
    }

    /**
     * The verb whose forms are least secure, so weak ones come round again.
     *
     * A verb counts as done when every one of its persons is mastered; among the rest the
     * one with the fewest attempts across its persons is asked next.
     */
    private fun List<VerbConjugation>.leastPractised(progress: Map<ConjugationVariant, ConjugationVariantProgress>): VerbConjugation? {
        fun attempts(verb: VerbConjugation) = verb.persons.sumOf { progress[ConjugationVariant(verb.infinitive, it)]?.attempted ?: 0 }

        fun mastered(verb: VerbConjugation) = verb.persons.all { progress[ConjugationVariant(verb.infinitive, it)]?.isMastered == true }

        return filterNot(::mastered).minByOrNull(::attempts)
    }

    private suspend fun ConjugationTable.withLearningAids(): ConjugationTable {
        val word = runCatching { vocabulary.findWordByText(infinitive) }.getOrNull()
        val subject = translation?.takeIf { it.isNotBlank() } ?: word?.translation ?: infinitive
        val image = runCatching { imageProvider.searchImage(subject) }.getOrNull()

        return copy(
            imageUrl = image,
            transcription = word?.transcription?.takeIf { it.isNotBlank() },
        )
    }
}

class EnsureVerbWordUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val createWord: CreateWordUseCase,
    private val imageProvider: ImageProvider,
) : EnsureVerbWordUseCase {
    override suspend fun invoke(
        infinitive: String,
        translation: String?,
    ): Long? {
        vocabulary.findWordByText(infinitive)?.let { return it.id.value }

        val english = translation?.trim().orEmpty()
        if (english.isEmpty()) return null

        val image = runCatching { imageProvider.searchImage(english) }.getOrNull()
        createWord(text = infinitive, translation = english, imageUrl = image, presetIds = emptyList())

        return vocabulary.findWordByText(infinitive)?.id?.value
    }
}

class LoadVerbImageChoicesUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : LoadVerbImageChoicesUseCase {
    override suspend fun invoke(
        infinitive: String,
        translation: String?,
    ): ImmutableList<String> {
        val subject = translation?.takeIf { it.isNotBlank() }
            ?: runCatching { vocabulary.findWordByText(infinitive) }.getOrNull()?.translation
            ?: infinitive

        return runCatching { imageProvider.searchImages(subject, CHOICE_COUNT) }
            .getOrDefault(emptyList())
            .toImmutableList()
    }

    private companion object {
        private const val CHOICE_COUNT = 12
    }
}

/**
 * Keeps the chosen picture where a word keeps its own.
 *
 * Pinning against the translation is what `CreateWordUseCase` does, so the verb's picture
 * is the word's picture: it shows in the vocabulary and the card as well as here, and
 * needs no table of its own.
 */
class ChooseVerbImageUseCaseImpl(
    private val ensureWord: EnsureVerbWordUseCase,
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : ChooseVerbImageUseCase {
    override suspend fun invoke(
        infinitive: String,
        translation: String?,
        imageUrl: String,
    ) {
        ensureWord(infinitive, translation)

        val subject = translation?.takeIf { it.isNotBlank() }
            ?: vocabulary.findWordByText(infinitive)?.translation
            ?: return

        imageProvider.pinImage(query = subject, imageUrl = imageUrl)
    }
}

class ToggleVerbInStudySetUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val ensureWord: EnsureVerbWordUseCase,
) : ToggleVerbInStudySetUseCase {
    override suspend fun invoke(
        infinitive: String,
        translation: String?,
        isInStudySet: Boolean,
    ) {
        val existing = vocabulary.findWordByText(infinitive)
        if (existing == null && !isInStudySet) return

        val id = existing?.id?.value ?: ensureWord(infinitive, translation) ?: return
        vocabulary.setInStudySet(listOf(id), isInStudySet)
    }
}

class LoadStudySetVerbsUseCaseImpl(
    private val vocabulary: VocabularyRepository,
) : LoadStudySetVerbsUseCase {
    override suspend fun invoke(infinitives: List<String>): Set<String> =
        infinitives.filter { vocabulary.findWordByText(it)?.isInStudySet == true }.toSet()
}

class SubmitConjugationAnswerUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : SubmitConjugationAnswerUseCase {
    override suspend fun invoke(request: SubmitConjugationAnswerRequest): SubmitConjugationAnswerResponse {
        val correctness = request.table.steps.associate { step ->
            val given = request.answers[step.variant.person]?.trim()
            val isCorrect = given != null && step.correctOptions.any { it.equalsAnswer(given) }

            conjugations.recordAttempt(
                courseId = request.courseId,
                infinitive = step.variant.infinitive,
                person = step.variant.person.sourceKey,
                isCorrect = isCorrect,
            )

            step.variant.person to isCorrect
        }

        return SubmitConjugationAnswerResponse(correctness = correctness)
    }
}

private fun String.equalsAnswer(other: String): Boolean = trim().equals(other.trim(), ignoreCase = true)
