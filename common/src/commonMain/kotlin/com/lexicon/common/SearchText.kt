package com.lexicon.common

private val POLISH_DIACRITICS = mapOf(
    'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
    'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
)

private val LATIN_ACCENTS = mapOf(
    'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a', 'ç' to 'c', 'č' to 'c',
    'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e', 'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
    'ñ' to 'n', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o', 'ø' to 'o', 'ř' to 'r', 'š' to 's',
    'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u', 'ý' to 'y', 'ÿ' to 'y', 'ž' to 'z',
)

fun String.foldForSearch(): String = lowercase().map { POLISH_DIACRITICS[it] ?: it }.joinToString("").trim()

fun String.foldAccentsForSearch(): String = foldForSearch().map { LATIN_ACCENTS[it] ?: it }.joinToString("")
