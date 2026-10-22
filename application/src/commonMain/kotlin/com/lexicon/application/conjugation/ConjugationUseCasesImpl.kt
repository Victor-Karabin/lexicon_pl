package com.lexicon.application.conjugation

import com.lexicon.application.paging.filteredPage
import com.lexicon.application.training.nextToPractise
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.ConjugationVariantProgress
import com.lexicon.interactors.conjugation.EnsureVerbWordUseCase
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.conjugation.LoadConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationTableUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerRequest
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerResponse
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.interactors.conjugation.VERB_COURSE_ID
import com.lexicon.interactors.conjugation.VerbConjugation
import com.lexicon.interactors.conjugation.VerbPage
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.presets.CreateWordUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

class LoadConjugationVerbsUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : LoadConjugationVerbsUseCase {
    /**
     * The store pages and filters, so only what the screen shows is decoded. Unteachable
     * verbs are the one thing SQL cannot judge — the forms live in a JSON column — so a
     * page thinned by them is topped up from the next, and the walk stops rather than
     * reading the whole table when a query matches almost nothing teachable.
     */
    override suspend fun page(
        query: String,
        offset: Int,
    ): VerbPage {
        val needle = query.trim()
        val page = filteredPage(
            offset = offset,
            fetch = { limit, from -> conjugations.verbPage(query = needle, limit = limit, offset = from) },
            accept = { row -> row.toVerb().takeIf { it.isTeachable } },
        )
        return VerbPage(verbs = page.items.toImmutableList(), nextOffset = page.nextOffset, isLast = page.isLast)
    }
}

class LoadConjugationCoursesUseCaseImpl(
    private val conjugations: ConjugationRepository,
    private val vocabulary: VocabularyRepository,
) : LoadConjugationCoursesUseCase {
    override suspend fun invoke(): ImmutableList<ConjugationCourse> {
        val verbs = conjugations.studySetVerbs(vocabulary)
        if (verbs.isEmpty()) return persistentListOf()
        return persistentListOf(
            ConjugationCourse(
                id = VERB_COURSE_ID,
                infinitives = verbs.map { it.infinitive }.toImmutableList(),
                progress = conjugations.courseProgress(VERB_COURSE_ID, verbs),
            ),
        )
    }
}

class NextConjugationTableUseCaseImpl(
    private val conjugations: ConjugationRepository,
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : NextConjugationTableUseCase {
    override suspend fun invoke(
        courseId: String,
        asked: Set<String>,
        random: Random,
    ): ConjugationTable? {
        val selected = conjugations.studySetVerbs(vocabulary)
        if (selected.isEmpty()) return null

        val progress = conjugations.courseProgress(courseId, selected).variants.associateBy { it.variant }
        val verb = selected.leastPractised(progress, asked, random) ?: return null

        return verb.question()?.withLearningAids()
    }

    private fun List<VerbConjugation>.leastPractised(
        progress: Map<ConjugationVariant, ConjugationVariantProgress>,
        asked: Set<String>,
        random: Random,
    ): VerbConjugation? {
        fun attempts(verb: VerbConjugation) = verb.persons.sumOf { progress[ConjugationVariant(verb.infinitive, it)]?.attempted ?: 0 }

        fun mastered(verb: VerbConjugation) = verb.persons.all { progress[ConjugationVariant(verb.infinitive, it)]?.isMastered == true }

        return filterNot(::mastered).nextToPractise(
            wasAsked = { it.infinitive in asked },
            isMastered = { false },
            attempts = ::attempts,
            random = random,
        )
    }

    private suspend fun ConjugationTable.withLearningAids(): ConjugationTable {
        val word = runSuspendCatching { vocabulary.findWordByText(infinitive) }.getOrNull()
        val subject = translation?.takeIf { it.isNotBlank() } ?: word?.translation ?: infinitive
        val image = runSuspendCatching { imageProvider.searchImage(subject) }.getOrNull()

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

        val image = runSuspendCatching { imageProvider.searchImage(english) }.getOrNull()
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
        skip: Int,
    ): ImmutableList<String> {
        val subject = translation?.takeIf { it.isNotBlank() }
            ?: runSuspendCatching { vocabulary.findWordByText(infinitive) }.getOrNull()?.translation
            ?: infinitive

        return runSuspendCatching { imageProvider.searchImages(subject, CHOICE_COUNT, skip) }
            .getOrDefault(emptyList())
            .toImmutableList()
    }

    private companion object {
        private const val CHOICE_COUNT = 12
    }
}

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

class SubmitConjugationAnswerUseCaseImpl(
    private val conjugations: ConjugationRepository,
) : SubmitConjugationAnswerUseCase {
    override suspend fun invoke(request: SubmitConjugationAnswerRequest): SubmitConjugationAnswerResponse {
        val verdicts = request.table.steps.associate { step ->
            val verdict = LessonAnswerChecker.verdict(step.forms, request.answers[step.variant.person].orEmpty())

            conjugations.recordAttempt(
                courseId = request.courseId,
                infinitive = step.variant.infinitive,
                person = step.variant.person.sourceKey,
                isCorrect = verdict == AnswerVerdict.CORRECT,
            )

            step.variant.person to verdict
        }

        return SubmitConjugationAnswerResponse(verdicts = verdicts)
    }
}
