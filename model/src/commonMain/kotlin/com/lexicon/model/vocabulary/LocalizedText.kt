package com.lexicon.model.vocabulary

data class LocalizedText(val values: Map<String, String>) {
    companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

fun LocalizedText.resolve(languageTag: String): String =
    values[languageTag]
        ?: values[LocalizedText.DEFAULT_LANGUAGE]
        ?: values.values.firstOrNull()
        ?: ""
