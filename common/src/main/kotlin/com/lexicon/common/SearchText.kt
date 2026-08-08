package com.lexicon.common

private val POLISH_DIACRITICS = mapOf(
    'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
    'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
)

fun String.foldForSearch(): String = lowercase().map { POLISH_DIACRITICS[it] ?: it }.joinToString("").trim()
