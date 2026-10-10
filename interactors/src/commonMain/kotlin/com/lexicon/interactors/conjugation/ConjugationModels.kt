package com.lexicon.interactors.conjugation

import com.lexicon.model.vocabulary.Aspect
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
    val example: String = "",
    val aspect: Aspect? = null,
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

data class ConjugationStep(
    val variant: ConjugationVariant,
    val forms: ImmutableList<String>,
) {
    val spokenForm: String get() = forms.first()
}

data class ConjugationTable(
    val infinitive: String,
    val translation: String? = null,
    val example: String = "",
    val steps: ImmutableList<ConjugationStep>,
    val imageUrl: String? = null,
    val transcription: String? = null,
    val aspect: Aspect? = null,
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

data class VerbPage(
    val verbs: ImmutableList<VerbConjugation>,
    val nextOffset: Int,
    val isLast: Boolean,
)
