package com.lexicon.application.declension

import com.lexicon.application.paging.filteredPage
import com.lexicon.application.training.nextToPractise
import com.lexicon.application.training.pictureOf
import com.lexicon.boundary.DeclensionRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.common.foldForSearch
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.interactors.declension.DECLENSION_COURSE_ID
import com.lexicon.interactors.declension.DeclensionColumn
import com.lexicon.interactors.declension.DeclensionCourse
import com.lexicon.interactors.declension.DeclensionCourseProgress
import com.lexicon.interactors.declension.DeclensionStep
import com.lexicon.interactors.declension.DeclensionTable
import com.lexicon.interactors.declension.DeclensionVariant
import com.lexicon.interactors.declension.DeclensionVariantProgress
import com.lexicon.interactors.declension.DeclinablePage
import com.lexicon.interactors.declension.DeclinableWord
import com.lexicon.interactors.declension.LoadDeclensionCoursesUseCase
import com.lexicon.interactors.declension.LoadDeclinableWordsUseCase
import com.lexicon.interactors.declension.NextDeclensionTableUseCase
import com.lexicon.interactors.declension.SubmitDeclensionAnswerRequest
import com.lexicon.interactors.declension.SubmitDeclensionAnswerResponse
import com.lexicon.interactors.declension.SubmitDeclensionAnswerUseCase
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

class LoadDeclinableWordsUseCaseImpl(
    private val declension: DeclensionRepository,
) : LoadDeclinableWordsUseCase {
    override suspend fun page(
        query: String,
        offset: Int,
    ): DeclinablePage {
        val needle = query.foldForSearch()
        val page = filteredPage(
            offset = offset,
            fetch = { limit, from -> declension.declinableWords(foldedQuery = needle, limit = limit, offset = from) },
            accept = { word -> word.takeIf { DeclinableWord.of(it) != null } },
        )
        return DeclinablePage(words = page.items.toImmutableList(), nextOffset = page.nextOffset, isLast = page.isLast)
    }
}

class LoadDeclensionCoursesUseCaseImpl(
    private val declension: DeclensionRepository,
) : LoadDeclensionCoursesUseCase {
    override suspend fun invoke(): ImmutableList<DeclensionCourse> {
        val words = declension.studySet().map { it.second }
        if (words.isEmpty()) return persistentListOf()
        return persistentListOf(
            DeclensionCourse(
                id = DECLENSION_COURSE_ID,
                words = words.map { it.text }.toImmutableList(),
                progress = declension.courseProgress(DECLENSION_COURSE_ID, words),
            ),
        )
    }
}

class NextDeclensionTableUseCaseImpl(
    private val declension: DeclensionRepository,
    private val imageProvider: ImageProvider,
) : NextDeclensionTableUseCase {
    override suspend fun invoke(
        courseId: String,
        asked: Set<String>,
        random: Random,
    ): DeclensionTable? {
        val studySet = declension.studySet()
        if (studySet.isEmpty()) return null

        val progress = declension.courseProgress(courseId, studySet.map { it.second }).variants.associateBy { it.variant }
        val (word, declinable, column) = studySet
            .flatMap { (word, declinable) -> declinable.columns.map { Triple(word, declinable, it) } }
            .filterNot { (_, declinable, column) -> declinable.isMastered(column, progress) }
            .nextToPractise(
                wasAsked = { (_, declinable, _) -> declinable.text in asked },
                isMastered = { false },
                attempts = { (_, declinable, column) -> declinable.attempts(column, progress) },
                random = random,
            )
            ?: return null

        val table = declinable.question(column) ?: return null
        val image = imageProvider.pictureOf(word)
        return table.copy(imageUrl = image, transcription = word.transcription.takeIf { it.isNotBlank() })
    }

    private fun DeclinableWord.variants(column: DeclensionColumn) = cases(column).map { DeclensionVariant(text, column, it) }

    private fun DeclinableWord.isMastered(
        column: DeclensionColumn,
        progress: Map<DeclensionVariant, DeclensionVariantProgress>,
    ): Boolean = variants(column).all { progress[it]?.isMastered == true }

    private fun DeclinableWord.attempts(
        column: DeclensionColumn,
        progress: Map<DeclensionVariant, DeclensionVariantProgress>,
    ): Int = variants(column).sumOf { progress[it]?.attempted ?: 0 }
}

class SubmitDeclensionAnswerUseCaseImpl(
    private val declension: DeclensionRepository,
) : SubmitDeclensionAnswerUseCase {
    override suspend fun invoke(request: SubmitDeclensionAnswerRequest): SubmitDeclensionAnswerResponse {
        val verdicts = request.table.steps.associate { step ->
            val verdict = LessonAnswerChecker.verdict(step.forms, request.answers[step.variant.case].orEmpty())

            declension.recordAttempt(
                courseId = request.courseId,
                word = step.variant.word,
                column = step.variant.column.tag,
                case = step.variant.case.tag,
                isCorrect = verdict == AnswerVerdict.CORRECT,
            )

            step.variant.case to verdict
        }

        return SubmitDeclensionAnswerResponse(verdicts = verdicts)
    }
}

internal suspend fun DeclensionRepository.studySet(): List<Pair<Word, DeclinableWord>> =
    studySetWords().mapNotNull { word -> DeclinableWord.of(word)?.let { word to it } }.distinctBy { it.second.text }

internal suspend fun DeclensionRepository.courseProgress(
    courseId: String,
    words: List<DeclinableWord>,
): DeclensionCourseProgress {
    val stored = progress(courseId).associateBy { Triple(it.word, it.column, it.case) }

    val variants = words.flatMap { word ->
        word.columns.flatMap { column ->
            word.cases(column).map { case ->
                val row = stored[Triple(word.text, column.tag, case.tag)]
                DeclensionVariantProgress(
                    variant = DeclensionVariant(word.text, column, case),
                    attempted = row?.attempted ?: 0,
                    correct = row?.correct ?: 0,
                    incorrect = row?.incorrect ?: 0,
                    streak = row?.streak ?: 0,
                )
            }
        }
    }

    return DeclensionCourseProgress(variants.toImmutableList())
}

internal fun DeclinableWord.question(column: DeclensionColumn): DeclensionTable? {
    val steps = cases(column).map { case: GrammaticalCase ->
        DeclensionStep(variant = DeclensionVariant(text, column, case), forms = formsFor(column, case))
    }
    if (steps.isEmpty()) return null

    return DeclensionTable(
        word = text,
        translation = translation,
        partOfSpeech = partOfSpeech,
        gender = gender,
        column = column,
        example = example,
        steps = steps.toImmutableList(),
    )
}
