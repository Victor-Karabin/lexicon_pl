package com.lexicon.interactors.declension

import com.lexicon.interactors.training.STUDY_SET_COURSE_ID
import com.lexicon.interactors.training.TABLE_DRILL_MASTERY_STREAK
import com.lexicon.model.vocabulary.CaseForms
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordForms
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

const val DECLENSION_COURSE_ID = STUDY_SET_COURSE_ID

enum class DeclensionColumn(val tag: String) {
    SINGULAR("singular"),
    PLURAL("plural"),
    MASCULINE("masculine"),
    FEMININE("feminine"),
    NEUTER("neuter"),
    PLURAL_PERSONAL("plural personal"),
    PLURAL_OTHER("plural other"),
}

data class DeclinableWord(
    val text: String,
    val translation: String,
    val partOfSpeech: PartOfSpeech,
    val gender: Gender? = null,
    val example: String = "",
    val tables: Map<DeclensionColumn, Map<GrammaticalCase, ImmutableList<String>>>,
) {
    val columns: List<DeclensionColumn>
        get() = DeclensionColumn.entries.filter { column -> tables[column].orEmpty().values.any { it.isNotEmpty() } }

    val isTeachable: Boolean get() = columns.isNotEmpty()

    fun cases(column: DeclensionColumn): List<GrammaticalCase> = GrammaticalCase.entries.filter { formsFor(column, it).isNotEmpty() }

    fun formsFor(
        column: DeclensionColumn,
        case: GrammaticalCase,
    ): ImmutableList<String> = tables[column]?.get(case) ?: persistentListOf()

    companion object {
        fun of(word: Word): DeclinableWord? {
            val text = word.text.trim()
            val tables = when (val forms = word.forms) {
                is WordForms.Noun -> forms.tables(text)
                is WordForms.Adjective -> PolishAdjectives.decline(forms)?.takeIf { forms.masculine.trim() == text }
                null -> null
            } ?: return null
            return DeclinableWord(
                text = text,
                translation = word.translation,
                partOfSpeech = if (word.forms is WordForms.Noun) PartOfSpeech.NOUN else PartOfSpeech.ADJECTIVE,
                gender = (word.forms as? WordForms.Noun)?.gender,
                example = word.example,
                tables = tables,
            ).takeIf { it.isTeachable }
        }

        private fun WordForms.Noun.tables(text: String): Map<DeclensionColumn, Map<GrammaticalCase, ImmutableList<String>>>? {
            val singular = declension.formsBy { it.singular }
            val plural = declension.formsBy { it.plural }
            val tables = if (gender == Gender.PLURAL_ONLY) {
                mapOf(DeclensionColumn.PLURAL to plural.ifEmpty { singular })
            } else {
                mapOf(DeclensionColumn.SINGULAR to singular, DeclensionColumn.PLURAL to plural)
            }
            val headword = tables.values.first()[GrammaticalCase.NOMINATIVE]
            return tables.takeIf { headword?.contains(text) == true }
        }

        private fun Map<GrammaticalCase, CaseForms>.formsBy(pick: (CaseForms) -> String?): Map<GrammaticalCase, ImmutableList<String>> =
            mapNotNull { (case, forms) -> pick(forms)?.trim()?.takeIf { it.isNotEmpty() }?.let { case to persistentListOf(it) } }
                .toMap()
    }
}

data class DeclensionVariant(
    val word: String,
    val column: DeclensionColumn,
    val case: GrammaticalCase,
)

data class DeclensionStep(
    val variant: DeclensionVariant,
    val forms: ImmutableList<String>,
) {
    val spokenForm: String get() = forms.first()
}

data class DeclensionTable(
    val word: String,
    val translation: String,
    val partOfSpeech: PartOfSpeech,
    val gender: Gender?,
    val column: DeclensionColumn,
    val example: String = "",
    val steps: ImmutableList<DeclensionStep>,
    val imageUrl: String? = null,
    val transcription: String? = null,
)

data class DeclensionVariantProgress(
    val variant: DeclensionVariant,
    val attempted: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val streak: Int = 0,
) {
    val isMastered: Boolean get() = streak >= TABLE_DRILL_MASTERY_STREAK
}

data class DeclensionCourseProgress(
    val variants: ImmutableList<DeclensionVariantProgress>,
) {
    val total: Int get() = variants.size

    val mastered: Int get() = variants.count { it.isMastered }

    val attempted: Int get() = variants.count { it.attempted > 0 }

    val words: Int get() = variants.map { it.variant.word }.distinct().size

    val fraction: Float get() = if (total == 0) 0f else mastered.toFloat() / total

    val isComplete: Boolean get() = total > 0 && mastered == total
}

data class DeclensionCourse(
    val id: String,
    val words: ImmutableList<String>,
    val progress: DeclensionCourseProgress,
)

data class DeclinablePage(
    val words: ImmutableList<Word>,
    val nextOffset: Int,
    val isLast: Boolean,
)

internal fun List<String>.toForms(): ImmutableList<String> = filter { it.isNotBlank() }.distinct().toImmutableList()
