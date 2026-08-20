package com.lexicon.model.vocabulary

data class VocabularyId(val value: Long)

enum class CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2,
    ;

    companion object {
        fun ofName(name: String?): CefrLevel? = entries.firstOrNull { it.name == name }
    }
}

data class Word(
    val id: VocabularyId,
    val text: String,
    val translation: String,
    val transcription: String,
    val isInStudySet: Boolean = false,
    val cefr: CefrLevel? = null,
) {
    init {
        require(text.isNotBlank()) { "a word must have text" }
    }

    val isPhrase: Boolean get() = text.contains(' ')

    fun addToStudySet(): Word = if (isInStudySet) this else copy(isInStudySet = true)

    fun removeFromStudySet(): Word = if (isInStudySet) copy(isInStudySet = false) else this

    fun edited(
        text: String = this.text,
        translation: String = this.translation,
        transcription: String = this.transcription,
    ): Word = copy(text = text, translation = translation, transcription = transcription)
}
