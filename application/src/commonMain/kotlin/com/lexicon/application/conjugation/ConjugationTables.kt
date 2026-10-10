package com.lexicon.application.conjugation

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.conjugation.ConjugationCourseProgress
import com.lexicon.interactors.conjugation.ConjugationStep
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.ConjugationVariant
import com.lexicon.interactors.conjugation.ConjugationVariantProgress
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.VerbConjugation
import kotlinx.collections.immutable.toImmutableList

internal fun VerbConjugationBoundary.toVerb(): VerbConjugation =
    VerbConjugation(
        infinitive = infinitive,
        translation = translation,
        example = example,
        aspect = aspect,
        forms = forms
            .mapNotNull { (key, values) ->
                GrammaticalPerson.bySourceKey(key)?.let { person -> person to values.toImmutableList() }
            }.toMap(),
    )

internal suspend fun ConjugationRepository.conjugationOf(infinitive: String): VerbConjugation? =
    verb(infinitive)?.toVerb()?.takeIf { it.isTeachable }

internal suspend fun ConjugationRepository.studySetVerbs(vocabulary: VocabularyRepository): List<VerbConjugation> {
    val teachable = verbs().map { it.toVerb() }.filter { it.isTeachable }
    val starred = vocabulary.studySetTextsAmong(teachable.map { it.infinitive })
    return teachable.filter { it.infinitive in starred }
}

internal suspend fun ConjugationRepository.courseProgress(
    courseId: String,
    verbs: List<VerbConjugation>,
): ConjugationCourseProgress {
    val stored = progress(courseId).associateBy { it.infinitive to it.person }

    val variants = verbs.flatMap { verb ->
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

internal fun VerbConjugation.question(): ConjugationTable? {
    val steps = persons.mapNotNull(::step)
    if (steps.isEmpty()) return null

    return ConjugationTable(
        infinitive = infinitive,
        translation = translation,
        example = example,
        aspect = aspect,
        steps = steps.toImmutableList(),
    )
}

internal fun VerbConjugation.step(person: GrammaticalPerson): ConjugationStep? {
    val forms = formsFor(person).filter { it.isNotBlank() }.distinct()
    if (forms.isEmpty()) return null
    return ConjugationStep(variant = ConjugationVariant(infinitive, person), forms = forms.toImmutableList())
}
