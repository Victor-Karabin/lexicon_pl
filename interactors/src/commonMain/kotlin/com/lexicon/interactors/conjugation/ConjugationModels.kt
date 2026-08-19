package com.lexicon.interactors.conjugation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The persons the source data distinguishes.
 *
 * `on`, `ona` and `ono` share one form in the source, as do `oni` and `one`, so they are
 * one variant here rather than several. Splitting them would mean inventing forms the
 * data does not contain.
 */
enum class GrammaticalPerson(
    val sourceKey: String,
    val label: String,
) {
    JA("ja", "ja"),
    TY("ty", "ty"),
    ON_ONA_ONO("on/ona/ono", "on / ona / ono"),
    MY("my", "my"),
    WY("wy", "wy"),
    ONI_ONE("oni/one", "oni / one"),
    ;

    companion object {
        fun bySourceKey(key: String): GrammaticalPerson? = entries.firstOrNull { it.sourceKey == key }
    }
}

data class VerbConjugation(
    val infinitive: String,
    val forms: Map<GrammaticalPerson, ImmutableList<String>>,
    val translation: String? = null,
) {
    val persons: List<GrammaticalPerson> get() = GrammaticalPerson.entries.filter { forms[it]?.isNotEmpty() == true }

    val isTeachable: Boolean get() = persons.isNotEmpty()

    val isComplete: Boolean get() = persons.size == GrammaticalPerson.entries.size

    fun formsFor(person: GrammaticalPerson): ImmutableList<String> = forms[person] ?: persistentListOf()
}

data class ConjugationVariant(
    val infinitive: String,
    val person: GrammaticalPerson,
)

enum class ConjugationAnswerMode { FULL_FORM, ENDING }

data class ConjugationStep(
    val variant: ConjugationVariant,
    val mode: ConjugationAnswerMode,
    val options: ImmutableList<String>,
    val correctOptions: ImmutableList<String>,
    val stem: String = "",
    val spokenForm: String = "",
)

data class ConjugationTable(
    val infinitive: String,
    val translation: String? = null,
    val steps: ImmutableList<ConjugationStep>,
    val bank: ImmutableList<String> = persistentListOf(),
    val imageUrl: String? = null,
    val transcription: String? = null,
)

data class ConjugationVariantProgress(
    val variant: ConjugationVariant,
    val attempted: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val streak: Int = 0,
) {
    val isMastered: Boolean get() = streak >= MASTERY_STREAK

    companion object {
        const val MASTERY_STREAK = 2
    }
}

data class ConjugationCourseProgress(
    val variants: ImmutableList<ConjugationVariantProgress>,
) {
    val total: Int get() = variants.size

    val mastered: Int get() = variants.count { it.isMastered }

    val attempted: Int get() = variants.count { it.attempted > 0 }

    val verbs: Int get() = variants.map { it.variant.infinitive }.distinct().size

    val fraction: Float get() = if (total == 0) 0f else mastered.toFloat() / total

    val isComplete: Boolean get() = total > 0 && mastered == total
}

data class ConjugationCourse(
    val id: String,
    val infinitives: ImmutableList<String>,
    val progress: ConjugationCourseProgress,
) {
    val title: String get() = infinitives.take(TITLE_VERBS).joinToString(", ")

    private companion object {
        private const val TITLE_VERBS = 3
    }
}
