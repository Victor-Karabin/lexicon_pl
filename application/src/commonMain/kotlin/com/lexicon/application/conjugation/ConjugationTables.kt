package com.lexicon.application.conjugation

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.interactors.conjugation.ConjugationAnswerMode
import com.lexicon.interactors.conjugation.ConjugationCourseProgress
import com.lexicon.interactors.conjugation.ConjugationStep
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.ConjugationVariantProgress
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.VerbConjugation
import kotlinx.collections.immutable.toImmutableList

internal const val OPTION_TARGET = 4

internal fun VerbConjugationBoundary.toVerb(): VerbConjugation =
    VerbConjugation(
        infinitive = infinitive,
        translation = translation,
        forms = forms
            .mapNotNull { (key, values) ->
                GrammaticalPerson.bySourceKey(key)?.let { person -> person to values.toImmutableList() }
            }.toMap(),
    )

internal suspend fun ConjugationRepository.courseVerbs(courseId: String): List<VerbConjugation> {
    val chosen = courses().firstOrNull { it.id == courseId }?.infinitives.orEmpty().toSet()
    return verbs().filter { it.infinitive in chosen }.map { it.toVerb() }.filter { it.isTeachable }
}

internal suspend fun ConjugationRepository.courseProgress(courseId: String): ConjugationCourseProgress {
    val stored = progress(courseId).associateBy { it.infinitive to it.person }

    val variants = courseVerbs(courseId).flatMap { verb ->
        verb.persons.map { person ->
            val row = stored[verb.infinitive to person.sourceKey]
            ConjugationVariantProgress(
                variant = ConjugationVariant(verb.infinitive, person),
                attempted = row?.attempted ?: 0,
                correct = row?.correct ?: 0,
                incorrect = row?.incorrect ?: 0,
                streak = row?.streak ?: 0,
            )
        }
    }

    return ConjugationCourseProgress(variants.toImmutableList())
}

/**
 * Builds one question covering every person the verb has a form for.
 *
 * Endings are asked for only where the verb's own forms decompose into a shared stem and
 * differing endings; where they do not — `być` being the standing example — whole forms
 * are asked for instead. Nothing here invents a form: every option is a string that
 * appears in the source data for some verb and person.
 */
internal fun VerbConjugation.question(pool: List<VerbConjugation>): ConjugationTable? {
    val steps = persons.mapNotNull { step(it, pool) }
    if (steps.isEmpty()) return null

    return ConjugationTable(
        infinitive = infinitive,
        translation = translation,
        steps = steps.toImmutableList(),
        bank = steps.flatMap { it.options }.distinct().shuffled().toImmutableList(),
    )
}

internal fun VerbConjugation.step(
    person: GrammaticalPerson,
    pool: List<VerbConjugation>,
): ConjugationStep? {
    val correct = formsFor(person)
    if (correct.isEmpty()) return null

    val split = split()
    val endings = split?.let { correct.mapNotNull(::endingFor) }.orEmpty()

    return if (split != null && endings.isNotEmpty()) {
        ConjugationStep(
            variant = ConjugationVariant(infinitive, person),
            mode = ConjugationAnswerMode.ENDING,
            options = optionsAround(endings, endingDistractors(person, pool)).toImmutableList(),
            correctOptions = endings.distinct().toImmutableList(),
            stem = split.stem,
            spokenForm = correct.first(),
        )
    } else {
        ConjugationStep(
            variant = ConjugationVariant(infinitive, person),
            mode = ConjugationAnswerMode.FULL_FORM,
            options = optionsAround(correct, formDistractors(person, pool)).toImmutableList(),
            correctOptions = correct.distinct().toImmutableList(),
            spokenForm = correct.first(),
        )
    }
}

/** Other persons of this verb first, then other verbs, never anything already correct. */
private fun VerbConjugation.formDistractors(
    person: GrammaticalPerson,
    pool: List<VerbConjugation>,
): List<String> {
    val own = persons.filterNot { it == person }.flatMap { formsFor(it) }
    val others = pool.filterNot { it.infinitive == infinitive }.flatMap { verb -> verb.persons.flatMap(verb::formsFor) }
    return own.shuffled() + others.shuffled()
}

private fun VerbConjugation.endingDistractors(
    person: GrammaticalPerson,
    pool: List<VerbConjugation>,
): List<String> {
    val own = persons.filterNot { it == person }.flatMap { formsFor(it) }.mapNotNull(::endingFor)
    val others = pool
        .filterNot { it.infinitive == infinitive }
        .flatMap { verb -> verb.persons.flatMap(verb::formsFor).mapNotNull(verb::endingFor) }
    return own.shuffled() + others.shuffled()
}

/**
 * The correct answers plus enough distinct wrong ones to fill the list.
 *
 * Blank values never reach here, duplicates are dropped, and nothing that is also a
 * correct answer can appear as a distractor — so an option list can never contain the
 * same string twice or offer two answers that are both right without both being marked so.
 */
private fun optionsAround(
    correct: List<String>,
    distractors: List<String>,
): List<String> {
    val answers = correct.filter { it.isNotBlank() }.distinct()
    if (answers.isEmpty()) return emptyList()

    val wrong = distractors
        .filter { it.isNotBlank() }
        .distinct()
        .filterNot { candidate -> answers.any { it.equals(candidate, ignoreCase = true) } }
        .take((OPTION_TARGET - answers.size).coerceAtLeast(0))

    return (answers + wrong).shuffled()
}
