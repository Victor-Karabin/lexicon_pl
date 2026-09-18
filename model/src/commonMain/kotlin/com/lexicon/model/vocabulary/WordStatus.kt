package com.lexicon.model.vocabulary

enum class WordStatus {
    UNDEFINED,
    TO_LEARN,
    FAVOURITE,
    KNOWN,
    ;

    val isLearning: Boolean get() = this == TO_LEARN || this == FAVOURITE

    fun next(): WordStatus = entries[(ordinal + 1) % entries.size]

    companion object {
        fun ofName(name: String?): WordStatus = entries.firstOrNull { it.name == name } ?: UNDEFINED
    }
}
